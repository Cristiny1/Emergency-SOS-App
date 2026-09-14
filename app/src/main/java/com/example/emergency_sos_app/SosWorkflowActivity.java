package com.example.emergency_sos_app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.emergency_sos_app.models.SosEvent;
import com.example.emergency_sos_app.models.SosStatus;
import com.example.emergency_sos_app.repositories.RepositoryProvider;
import com.example.emergency_sos_app.repositories.SosRepository;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Dynamic SOS Workflow with real-time tracking, radar effects, and smart timeline.
 */
public class SosWorkflowActivity extends BaseActivity {

    private enum State { LOCATION, ROUTING, TRACKING, COMPLETED }
    private State currentState = State.LOCATION;

    private LinearLayout phaseLocation, phaseTracking, statusTimeline;
    private View responderCard;
    private TextView tvCurrentStatus, tvLocationText, tvResponderName, tvResponderId, tvEta, tvHeaderTitle;
    private ProgressBar pbRouting;
    private Spinner spinnerVictims;
    private Button btnConfirmLocation, btnCancelSos, btnMuteSiren;
    private ImageView btnBack, btnCallResponder;
    private View vAlertOverlay;

    private WebView mapWebView;
    private boolean isMapLoaded = false;
    private boolean isLocationLocked = false;
    private double userLat = 13.3633, userLng = 103.8564;

    private BottomSheetBehavior<View> bottomSheetBehavior;
    private SosAlertManager alertManager;
    private SocketManager socketManager;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private boolean isMuted = false;
    private boolean isCancelled = false;
    private boolean isSilentMode = false;
    private int cancelCountdown = 10;
    private String sosId;
    private SosStatus currentRepositoryStatus = SosStatus.IDLE;
    
    private String incidentType = "MEDICAL";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private View rootLayout;

    private final android.content.BroadcastReceiver batteryReceiver = new android.content.BroadcastReceiver() {
        @Override
        public void onReceive(android.content.Context context, Intent intent) {
            int level = intent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1);
            float batteryPct = level * 100 / (float)scale;

            if (batteryPct < 15) {
                applyLowPowerMode();
            }
        }
    };

    private void applyLowPowerMode() {
        if (currentState == State.TRACKING || currentState == State.ROUTING) {
            Toast.makeText(this, "Low Battery: Emergency Power Saving Active", Toast.LENGTH_LONG).show();
            if (isMapLoaded && mapWebView != null) {
                mapWebView.evaluateJavascript("stopRadar()", null);
            }
            if (rootLayout != null) rootLayout.setAlpha(0.8f);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("Workflow", "onCreate started");
        
        try {
            setContentView(R.layout.activity_sos_workflow);
            rootLayout = findViewById(android.R.id.content);

            incidentType = getIntent().getStringExtra("INCIDENT_TYPE");
            if (incidentType == null) incidentType = "MEDICAL";
            
            isSilentMode = getIntent().getBooleanExtra("IS_SILENT", false);
            
            sosId = getIntent().getStringExtra("SOS_ID");
            if (sosId != null) {
                Log.d("Workflow", "Initial State set to ROUTING due to SOS_ID");
                currentState = State.ROUTING;
            }

            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            initViews();
            setupWebView();

            alertManager = new SosAlertManager(this);
            socketManager = SocketManager.getInstance();

            updateUI();

            setupListeners();
            setupSocketListeners();
            requestNecessaryPermissions();
            
            // Lockdown: Prevent accidental exit
            getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    if (currentState == State.ROUTING || currentState == State.TRACKING) {
                        Toast.makeText(SosWorkflowActivity.this, "Emergency is active. Use Cancel button to exit.", Toast.LENGTH_SHORT).show();
                    } else {
                        setEnabled(false);
                        getOnBackPressedDispatcher().onBackPressed();
                    }
                }
            });

            registerReceiver(batteryReceiver, new android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED));

            if (sosId != null) {
                handler.postDelayed(this::observeSosStatus, 500);
            }
        } catch (Throwable e) {
            Log.e("Workflow", "Fatal crash in onCreate", e);
            String detail = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            Toast.makeText(this, "SOS Workflow Error: " + detail, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void setupListeners() {
        if (btnConfirmLocation != null) btnConfirmLocation.setOnClickListener(v -> startSosWorkflow());
        if (btnCancelSos != null) btnCancelSos.setOnClickListener(v -> attemptCancelEmergency());
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        
        View btnCenterMap = findViewById(R.id.btnCenterMap);
        if (btnCenterMap != null) btnCenterMap.setOnClickListener(v -> updateMapLocation());
    }

    private void observeSosStatus() {
        if (isFinishing() || isDestroyed() || sosId == null) return;
        Log.d("Workflow", "Requesting current status from Repository");
        
        RepositoryProvider.getSosRepository(this).getSosStatus(sosId, new SosRepository.SosCallback() {
            @Override
            public void onStatusChanged(SosStatus status) {
                handler.post(() -> {
                    Log.d("Workflow", "Repository Status changed to: " + status);
                    handleStatusUpdate(status);
                });
            }

            @Override
            public void onError(String message) {
                Log.e("Workflow", "Repository error: " + message);
            }
        });
    }

    private void handleStatusUpdate(SosStatus status) {
        if (isFinishing() || isDestroyed()) return;
        this.currentRepositoryStatus = status;
        
        // Map Repository Status to Workflow State
        if (status == SosStatus.PENDING || status == SosStatus.SENT || status == SosStatus.ACKNOWLEDGED) {
            currentState = State.ROUTING;
        } else if (status == SosStatus.RESPONDER_ASSIGNED || status == SosStatus.RESPONDER_EN_ROUTE || status == SosStatus.ARRIVED) {
            currentState = State.TRACKING;
        } else if (status == SosStatus.RESOLVED) {
            currentState = State.COMPLETED;
            clearPersistentSos();
        } else if (status == SosStatus.CANCELLED || status == SosStatus.FAILED) {
            clearPersistentSos();
            finish();
            return;
        }

        // Show Responder Card once assigned (and keep it shown for subsequent tracking states)
        if (status == SosStatus.RESPONDER_ASSIGNED || status == SosStatus.RESPONDER_EN_ROUTE || status == SosStatus.ARRIVED) {
            if (responderCard != null && responderCard.getVisibility() != View.VISIBLE) {
                responderCard.setVisibility(View.VISIBLE);
                responderCard.setAlpha(1.0f);
                if (bottomSheetBehavior != null) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                }
            }
            if (tvEta != null) tvEta.setVisibility(View.VISIBLE);
            if (tvResponderName != null) tvResponderName.setText(R.string.tracking_default_agent_name);
        }

        updateUI();
    }

    private void clearPersistentSos() {
        getSharedPreferences("sos_profile_prefs", MODE_PRIVATE).edit()
                .putBoolean("is_sos_active", false)
                .remove("active_sos_id")
                .remove("active_sos_type")
                .apply();
    }

    private void attemptCancelEmergency() {
        if (currentState == State.COMPLETED) {
            finish();
            return;
        }

        BiometricManager biometricManager = BiometricManager.from(this);
        int canAuth = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL);
        
        if (canAuth == BiometricManager.BIOMETRIC_SUCCESS) {
            BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle(getString(R.string.verify_identity))
                    .setSubtitle(getString(R.string.confirm_cancel_subtitle))
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                    .build();

            BiometricPrompt biometricPrompt = new BiometricPrompt(this, ContextCompat.getMainExecutor(this),
                    new BiometricPrompt.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                            super.onAuthenticationSucceeded(result);
                            cancelEmergency();
                        }

                        @Override
                        public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                            super.onAuthenticationError(errorCode, errString);
                            Toast.makeText(SosWorkflowActivity.this, getString(R.string.security_verification_failed), Toast.LENGTH_SHORT).show();
                        }
                    });

            biometricPrompt.authenticate(promptInfo);
        } else {
            // Fallback for devices without biometrics/lock
            cancelEmergency();
        }
    }

    private void cancelEmergency() {
        Log.d("Workflow", "cancelEmergency clicked. State: " + currentState);
        isCancelled = true;
        
        // Immediately clear persistent state so user isn't trapped in SOS loop
        clearPersistentSos();

        if (sosId != null && currentState != State.COMPLETED) {
            RepositoryProvider.getSosRepository(this).cancelSos(sosId, new SosRepository.SosCallback() {
                @Override public void onStatusChanged(SosStatus status) {
                    // Status already cleared locally
                }
                @Override public void onError(String message) {}
            });
        }
        
        navigateToDashboard();
    }

    private void navigateToDashboard() {
        // If this activity is the task root (e.g. started from Splash), 
        // we must start Dashboard explicitly or the app will close.
        if (isTaskRoot()) {
            Intent intent = new Intent(this, DashboardActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
        finish();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        mapWebView = findViewById(R.id.mapView);
        if (mapWebView == null) return;
        
        WebSettings settings = mapWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        mapWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                isMapLoaded = true;
                updateMapLocation();
                if (currentState == State.ROUTING || currentState == State.TRACKING) {
                    mapWebView.evaluateJavascript("startRadar()", null);
                }
            }
        });

        mapWebView.loadUrl("file:///android_asset/leaflet_map.html");
    }

    private void updateMapLocation() {
        if (!isMapLoaded || mapWebView == null) return;
        mapWebView.evaluateJavascript("setLocation(" + userLat + "," + userLng + ", 'Your Location')", null);
    }

    private void requestNecessaryPermissions() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        List<String> toRequest = new ArrayList<>();
        for (String p : permissions) {
            if (ActivityCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                toRequest.add(p);
            }
        }

        if (!toRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this, toRequest.toArray(new String[0]), 101);
        } else {
            detectCurrentLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            detectCurrentLocation();
        }
    }

    private void detectCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            
            LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                    .setMinUpdateDistanceMeters(2)
                    .build();

            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(@NonNull LocationResult locationResult) {
                    for (android.location.Location location : locationResult.getLocations()) {
                        userLat = location.getLatitude();
                        userLng = location.getLongitude();
                        isLocationLocked = true;
                        updateMapLocation();
                        
                        float accuracy = location.getAccuracy();
                        if (tvLocationText != null) {
                            String accuracyStatus;
                            int color;
                            if (accuracy <= 10) {
                                accuracyStatus = getString(R.string.gps_excellent);
                                color = getColor(R.color.green_verified);
                            } else if (accuracy <= 25) {
                                accuracyStatus = getString(R.string.gps_good);
                                color = getColor(R.color.green_verified);
                            } else if (accuracy <= 50) {
                                accuracyStatus = getString(R.string.gps_fair);
                                color = getColor(R.color.severity_yellow);
                            } else {
                                accuracyStatus = getString(R.string.gps_poor);
                                color = getColor(R.color.sos_red);
                            }
                            
                            String locDetail = String.format(Locale.getDefault(), "Live Lat: %.5f, Lng: %.5f\nGPS Accuracy: %.0fm (%s)", 
                                    userLat, userLng, accuracy, accuracyStatus);
                            tvLocationText.setText(locDetail);
                            tvLocationText.setTextColor(color);
                            
                            if (accuracy > 25) {
                                Toast.makeText(SosWorkflowActivity.this, "Low GPS Accuracy. Move to open area.", Toast.LENGTH_SHORT).show();
                            }
                        }
                        
                        if (btnConfirmLocation != null && currentState == State.LOCATION) {
                            btnConfirmLocation.setEnabled(true);
                            btnConfirmLocation.setText(R.string.send_sos_signal);
                            btnConfirmLocation.setAlpha(1.0f);
                        }
                    }
                }
            };

            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());

            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    userLat = location.getLatitude();
                    userLng = location.getLongitude();
                    updateMapLocation();
                }
            });
        }
    }

    private void initViews() {
        phaseLocation = findViewById(R.id.phaseLocation);
        phaseTracking = findViewById(R.id.phaseTracking);
        responderCard = findViewById(R.id.responderCard);
        statusTimeline = findViewById(R.id.statusTimeline);

        tvCurrentStatus = findViewById(R.id.tvCurrentStatus);
        tvLocationText = findViewById(R.id.tvLocationText);
        tvResponderName = findViewById(R.id.tvResponderName);
        tvResponderId = findViewById(R.id.tvResponderId);
        tvEta = findViewById(R.id.tvEta);
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);

        pbRouting = findViewById(R.id.pbRouting);
        spinnerVictims = findViewById(R.id.spinnerVictims);
        btnConfirmLocation = findViewById(R.id.btnConfirmLocation);
        btnCancelSos = findViewById(R.id.btnCancelSos);
        btnBack = findViewById(R.id.btnBack);
        btnCallResponder = findViewById(R.id.btnCallResponder);
        btnMuteSiren = findViewById(R.id.btnMuteSiren);
        vAlertOverlay = findViewById(R.id.vAlertOverlay);

        View bottomSheet = findViewById(R.id.bottomSheet);
        if (bottomSheet != null) {
            bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        }

        if (btnMuteSiren != null) btnMuteSiren.setOnClickListener(v -> toggleMute());
    }

    private void setupSocketListeners() {
        socketManager.on("status_update", args -> {
            if (args.length > 0) {
                try {
                    JSONObject data = (JSONObject) args[0];
                    String statusStr = data.getString("status");
                    SosStatus status = SosStatus.valueOf(statusStr.toUpperCase().trim());
                    handler.post(() -> handleStatusUpdate(status));
                } catch (Exception e) {
                    Log.e("Workflow", "Socket status parse error", e);
                }
            }
        });

        socketManager.on("agent_location", args -> {
            if (args.length > 0) {
                try {
                    JSONObject data = (JSONObject) args[0];
                    double lat = data.getDouble("lat");
                    double lng = data.getDouble("lng");
                    handler.post(() -> updateResponderOnMap(lat, lng));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void updateResponderOnMap(double lat, double lng) {
        if (!isMapLoaded || mapWebView == null) return;
        mapWebView.evaluateJavascript("setResponder(" + lat + "," + lng + ", 'Rescue Team')", null);
        mapWebView.evaluateJavascript("updateRoute(" + lat + "," + lng + "," + userLat + "," + userLng + ")", null);
        mapWebView.evaluateJavascript("fitToMarkers(" + userLat + "," + userLng + "," + lat + "," + lng + ")", null);
    }

    private void startSosWorkflow() {
        Log.d("Workflow", "startSosWorkflow");
        currentState = State.ROUTING;
        updateUI();

        if (isMapLoaded && mapWebView != null) {
            mapWebView.evaluateJavascript("startRadar()", null);
        }

        SharedPreferences prefs = getSharedPreferences("sos_profile_prefs", MODE_PRIVATE);
        String userName = prefs.getString("name", "User");

        // Use Repository to trigger SOS
        sosId = "SOS_" + System.currentTimeMillis();
        SosEvent event = new SosEvent(sosId, userName, incidentType, userLat, userLng, 0);
        
        RepositoryProvider.getSosRepository(this).createSos(event, new SosRepository.SosCallback() {
            @Override
            public void onStatusChanged(SosStatus status) {
                handler.post(() -> {
                    Log.d("Workflow", "startSosWorkflow: Callback Status -> " + status);
                    handleStatusUpdate(status);
                });
            }

            @Override
            public void onError(String message) {
                Log.e("Workflow", "createSos error: " + message);
                handler.post(() -> Toast.makeText(SosWorkflowActivity.this, message, Toast.LENGTH_SHORT).show());
            }
        });

        if (alertManager != null) {
            alertManager.startSiren(isSilentMode);
            alertManager.showCenterAlert(userName, incidentType);
        }
        if (bottomSheetBehavior != null) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }

        // Start Cancellation Window Logic
        startCancellationTimer();
    }

    private void startCancellationTimer() {
        cancelCountdown = 10;
        Runnable timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (isCancelled || currentState == State.COMPLETED) return;
                
                if (cancelCountdown > 0) {
                    if (btnCancelSos != null) btnCancelSos.setText(getString(R.string.cancel_sos_countdown, cancelCountdown));
                    cancelCountdown--;
                    handler.postDelayed(this, 1000);
                } else {
                    if (btnCancelSos != null) btnCancelSos.setText(R.string.cancel_emergency);
                }
            }
        };
        handler.post(timerRunnable);
    }

    private void updateUI() {
        if (phaseLocation != null) phaseLocation.setVisibility(currentState == State.LOCATION ? View.VISIBLE : View.GONE);
        if (phaseTracking != null) phaseTracking.setVisibility(currentState != State.LOCATION ? View.VISIBLE : View.GONE);
        if (vAlertOverlay != null) vAlertOverlay.setVisibility(currentState != State.LOCATION && currentState != State.COMPLETED ? View.VISIBLE : View.GONE);

        if (currentState == State.ROUTING) {
            if (tvCurrentStatus != null) {
                if (currentRepositoryStatus == SosStatus.PENDING) {
                    tvCurrentStatus.setText(R.string.broadcasting_signal);
                } else if (currentRepositoryStatus == SosStatus.SENT) {
                    tvCurrentStatus.setText(R.string.signal_reached_center);
                } else if (currentRepositoryStatus == SosStatus.ACKNOWLEDGED) {
                    tvCurrentStatus.setText(R.string.awaiting_responder);
                } else {
                    tvCurrentStatus.setText(R.string.broadcasting_signal);
                }
                tvCurrentStatus.setTextColor(getColor(R.color.sos_red));
            }
            if (pbRouting != null) pbRouting.setVisibility(View.VISIBLE);
            if (tvHeaderTitle != null) tvHeaderTitle.setText(R.string.active_sos_header);
            startHeaderPulse();
        } else if (currentState == State.TRACKING) {
            if (tvCurrentStatus != null) tvCurrentStatus.setText(R.string.rescue_on_the_way);
            if (pbRouting != null) pbRouting.setVisibility(View.GONE);
            if (isMapLoaded && mapWebView != null) mapWebView.evaluateJavascript("startRadar()", null);
        } else if (currentState == State.COMPLETED) {
            if (tvCurrentStatus != null) {
                tvCurrentStatus.setText(R.string.emergency_assisted_closed);
                tvCurrentStatus.setTextColor(getColor(R.color.green_verified));
            }
            if (pbRouting != null) pbRouting.setVisibility(View.GONE);
            if (btnCancelSos != null) {
                btnCancelSos.setText(R.string.return_to_dashboard);
                btnCancelSos.setTextColor(getColor(R.color.fb_blue));
            }
            if (btnBack != null) btnBack.setVisibility(View.VISIBLE);
            if (alertManager != null) alertManager.stopSiren();
            if (isMapLoaded && mapWebView != null) mapWebView.evaluateJavascript("stopRadar()", null);
        }
        
        buildTimeline();
    }

    private void startHeaderPulse() {
        if (tvHeaderTitle == null) return;
        Animation anim = new AlphaAnimation(1.0f, 0.4f);
        anim.setDuration(800);
        anim.setRepeatMode(Animation.REVERSE);
        anim.setRepeatCount(Animation.INFINITE);
        tvHeaderTitle.startAnimation(anim);
    }

    private void buildTimeline() {
        if (statusTimeline == null) return;
        statusTimeline.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        String[] steps = {
                getString(R.string.step_signal_sent),
                getString(R.string.step_responder_assigned),
                getString(R.string.step_help_enroute),
                getString(R.string.step_arrived)
        };
        
        // Refined logical progression for checkmarks
        boolean[] completed = {
                currentRepositoryStatus == SosStatus.PENDING || currentRepositoryStatus == SosStatus.SENT || currentRepositoryStatus == SosStatus.ACKNOWLEDGED || currentRepositoryStatus.ordinal() > SosStatus.ACKNOWLEDGED.ordinal(),
                currentRepositoryStatus == SosStatus.RESPONDER_ASSIGNED || currentRepositoryStatus.ordinal() > SosStatus.RESPONDER_ASSIGNED.ordinal(),
                currentRepositoryStatus == SosStatus.RESPONDER_EN_ROUTE || currentRepositoryStatus.ordinal() > SosStatus.RESPONDER_EN_ROUTE.ordinal(),
                currentRepositoryStatus == SosStatus.ARRIVED || currentRepositoryStatus == SosStatus.RESOLVED
        };

        for (int i = 0; i < steps.length; i++) {
            View stepView = inflater.inflate(R.layout.item_sos_status, statusTimeline, false);
            TextView title = stepView.findViewById(R.id.tvStatusTitle);
            TextView desc = stepView.findViewById(R.id.tvStatusDesc);
            ImageView dot = stepView.findViewById(R.id.ivStatusDot);
            View line = stepView.findViewById(R.id.vStatusLine);

            if (title != null) title.setText(steps[i]);
            if (i == steps.length - 1 && line != null) line.setVisibility(View.GONE);

            if (completed[i]) {
                if (title != null) title.setAlpha(1.0f);
                if (desc != null) desc.setAlpha(1.0f);
                if (dot != null) {
                    dot.setAlpha(1.0f);
                    dot.setImageResource(R.drawable.ic_check);
                    dot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.green_verified)));
                }
                if (desc != null) desc.setText(R.string.completed);
            } else if (i > 0 && completed[i-1]) {
                if (title != null) title.setAlpha(1.0f);
                if (desc != null) desc.setAlpha(1.0f);
                if (dot != null) {
                    dot.setAlpha(1.0f);
                    dot.setImageResource(R.drawable.ic_clock);
                    Animation pulse = new AlphaAnimation(1.0f, 0.3f);
                    pulse.setDuration(1000);
                    pulse.setRepeatMode(Animation.REVERSE);
                    pulse.setRepeatCount(Animation.INFINITE);
                    dot.startAnimation(pulse);
                }
                if (desc != null) desc.setText(R.string.in_progress);
            }

            statusTimeline.addView(stepView);
        }
    }

    private void callResponder() {
        startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:012345678")));
    }

    private void toggleMute() {
        isMuted = !isMuted;
        if (isMuted) {
            if (alertManager != null) alertManager.stopSiren();
            if (btnMuteSiren != null) btnMuteSiren.setText(R.string.unmute_siren);
        } else {
            if (alertManager != null) alertManager.startSiren();
            if (btnMuteSiren != null) btnMuteSiren.setText(R.string.mute_siren);
        }
    }

    @Override protected void onResume() { super.onResume(); }
    @Override protected void onPause() {
        super.onPause();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(batteryReceiver);
            if (alertManager != null) {
                alertManager.stopSiren();
                alertManager.clearNotifications();
            }
            if (socketManager != null) {
                socketManager.off("status_update");
                socketManager.off("agent_location");
            }
            if (handler != null) {
                handler.removeCallbacksAndMessages(null);
            }
        } catch (Exception e) {
            Log.e("Workflow", "Error in onDestroy clean-up", e);
        }
    }
}
