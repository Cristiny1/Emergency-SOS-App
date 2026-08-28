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
import android.transition.TransitionManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

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

    private ViewGroup rootLayout;
    private LinearLayout phaseLocation, phaseTracking, responderCard, statusTimeline;
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
    private int cancelCountdown = 10;
    private String sosId;
    
    private String incidentType = "MEDICAL";
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sos_workflow);

        incidentType = getIntent().getStringExtra("INCIDENT_TYPE");
        if (incidentType == null) incidentType = "MEDICAL";
        
        sosId = getIntent().getStringExtra("SOS_ID");

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        initViews();
        setupWebView();

        alertManager = new SosAlertManager(this);
        socketManager = SocketManager.getInstance();

        updateUI();

        btnConfirmLocation.setOnClickListener(v -> startSosWorkflow());
        btnCancelSos.setOnClickListener(v -> cancelEmergency());
        btnBack.setOnClickListener(v -> finish());
        btnCallResponder.setOnClickListener(v -> callResponder());
        findViewById(R.id.btnCenterMap).setOnClickListener(v -> updateMapLocation());

        setupSocketListeners();
        requestNecessaryPermissions();
        
        if (sosId != null) {
            observeSosStatus();
        }
    }

    private void observeSosStatus() {
        RepositoryProvider.getSosRepository().getSosStatus(sosId, new SosRepository.SosCallback() {
            @Override
            public void onStatusChanged(SosStatus status) {
                runOnUiThread(() -> handleStatusUpdate(status));
            }

            @Override
            public void onError(String message) {
                Log.e("SOS", "Status Error: " + message);
            }
        });
    }

    private void cancelEmergency() {
        isCancelled = true;
        if (sosId != null) {
            RepositoryProvider.getSosRepository().cancelSos(sosId, new SosRepository.SosCallback() {
                @Override public void onStatusChanged(SosStatus status) {
                    runOnUiThread(() -> updateUI());
                }
                @Override public void onError(String message) {}
            });
        }
        finish();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        mapWebView = findViewById(R.id.mapView);
        WebSettings settings = mapWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        mapWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                isMapLoaded = true;
                updateMapLocation();
            }
        });

        mapWebView.loadUrl("file:///android_asset/leaflet_map.html");
    }

    private void updateMapLocation() {
        if (!isMapLoaded) return;
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
                            if (accuracy > 25) {
                                tvLocationText.setText(String.format(Locale.getDefault(), 
                                    "Low GPS Accuracy (%.0fm). Move to open area.", accuracy));
                                tvLocationText.setTextColor(getColor(R.color.sos_red));
                            } else {
                                tvLocationText.setText(String.format(Locale.getDefault(), "Live Lat: %.5f, Lng: %.5f", userLat, userLng));
                                tvLocationText.setTextColor(getColor(R.color.text_secondary));
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
        rootLayout = findViewById(android.R.id.content);
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
        btnConfirmLocation.setEnabled(false);
        btnConfirmLocation.setText(R.string.waiting_for_gps);
        btnConfirmLocation.setAlpha(0.6f);
        btnCancelSos = findViewById(R.id.btnCancelSos);
        btnBack = findViewById(R.id.btnBack);
        btnCallResponder = findViewById(R.id.btnCallResponder);
        btnMuteSiren = findViewById(R.id.btnMuteSiren);
        vAlertOverlay = findViewById(R.id.vAlertOverlay);

        View bottomSheet = findViewById(R.id.bottomSheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);

        btnMuteSiren.setOnClickListener(v -> toggleMute());
    }

    private void setupSocketListeners() {
        socketManager.on("status_update", args -> {
            if (args.length > 0) {
                try {
                    JSONObject data = (JSONObject) args[0];
                    String statusStr = data.getString("status");
                    
                    // Map string to Enum
                    SosStatus status = SosStatus.valueOf(statusStr);
                    handler.post(() -> handleStatusUpdate(status));
                } catch (Exception e) {
                    e.printStackTrace();
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
        if (!isMapLoaded) return;
        mapWebView.evaluateJavascript("setResponder(" + lat + "," + lng + ", 'Rescue Team')", null);
        mapWebView.evaluateJavascript("updateRoute(" + lat + "," + lng + "," + userLat + "," + userLng + ")", null);
        mapWebView.evaluateJavascript("fitToMarkers(" + userLat + "," + userLng + "," + lat + "," + lng + ")", null);
    }

    private void handleStatusUpdate(SosStatus status) {
        switch (status) {
            case SENT:
            case ACKNOWLEDGED:
                currentState = State.ROUTING;
                updateUI();
                break;
            case RESPONDER_ASSIGNED:
                currentState = State.TRACKING;
                responderCard.setVisibility(View.VISIBLE);
                responderCard.setAlpha(1.0f);
                tvEta.setVisibility(View.VISIBLE);
                tvResponderName.setText(R.string.tracking_default_agent_name);
                tvResponderId.setText("Unit #SIM-101");
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                updateUI();
                break;
            case RESPONDER_EN_ROUTE:
                currentState = State.TRACKING;
                updateUI();
                break;
            case ARRIVED:
                currentState = State.TRACKING;
                updateUI();
                break;
            case RESOLVED:
                currentState = State.COMPLETED;
                updateUI();
                break;
        }
    }

    private void startSosWorkflow() {
        TransitionManager.beginDelayedTransition(rootLayout);
        currentState = State.ROUTING;
        updateUI();

        if (isMapLoaded) {
            mapWebView.evaluateJavascript("startRadar()", null);
        }

        SharedPreferences prefs = getSharedPreferences("sos_profile_prefs", MODE_PRIVATE);
        String userName = prefs.getString("name", "User");
        String victims = (spinnerVictims != null && spinnerVictims.getSelectedItem() != null)
                ? spinnerVictims.getSelectedItem().toString()
                : "1";

        socketManager.connect(prefs.getString("sos_access_token", null));

        // Create Robust SOS Payload
        JSONObject sosData = new JSONObject();
        try {
            sosData.put("event", "SOS_CREATED");
            sosData.put("sosId", "SOS_" + System.currentTimeMillis());
            sosData.put("type", incidentType);
            sosData.put("victims", victims);
            sosData.put("userName", userName);
            sosData.put("latitude", userLat);
            sosData.put("longitude", userLng);
            sosData.put("timestamp", System.currentTimeMillis());
            sosData.put("status", "SIGNAL_SENT");
            
            socketManager.emit("sos_request", sosData);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        alertManager.startSiren();
        alertManager.showCenterAlert(userName, incidentType);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        // Start Cancellation Window Logic
        startCancellationTimer();
        
        // Listen to the new SOS session
        observeSosStatus();
    }

    private void startCancellationTimer() {
        cancelCountdown = 10;
        Runnable timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (isCancelled || currentState == State.COMPLETED) return;
                
                if (cancelCountdown > 0) {
                    btnCancelSos.setText(getString(R.string.cancel_sos_countdown, cancelCountdown));
                    cancelCountdown--;
                    handler.postDelayed(this, 1000);
                } else {
                    btnCancelSos.setText(R.string.cancel_emergency);
                }
            }
        };
        handler.post(timerRunnable);
    }

    private void updateUI() {
        TransitionManager.beginDelayedTransition(rootLayout);
        phaseLocation.setVisibility(currentState == State.LOCATION ? View.VISIBLE : View.GONE);
        phaseTracking.setVisibility(currentState != State.LOCATION ? View.VISIBLE : View.GONE);
        vAlertOverlay.setVisibility(currentState != State.LOCATION && currentState != State.COMPLETED ? View.VISIBLE : View.GONE);

        if (currentState == State.ROUTING) {
            tvCurrentStatus.setText(R.string.broadcasting_signal);
            tvCurrentStatus.setTextColor(getColor(R.color.sos_red));
            pbRouting.setVisibility(View.VISIBLE);
            tvHeaderTitle.setText(R.string.active_sos_header);
            startHeaderPulse();
        } else if (currentState == State.TRACKING) {
            tvCurrentStatus.setText(R.string.rescue_on_the_way);
            pbRouting.setVisibility(View.GONE);
        } else if (currentState == State.COMPLETED) {
            tvCurrentStatus.setText(R.string.emergency_assisted_closed);
            tvCurrentStatus.setTextColor(getColor(R.color.green_verified));
            pbRouting.setVisibility(View.GONE);
            btnCancelSos.setText(R.string.return_to_dashboard);
            btnCancelSos.setTextColor(getColor(R.color.fb_blue));
            btnBack.setVisibility(View.VISIBLE);
            alertManager.stopSiren();
            if (isMapLoaded) mapWebView.evaluateJavascript("stopRadar()", null);
        }
        
        buildTimeline();
    }

    private void startHeaderPulse() {
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

        String[] steps = {"Signal Sent", "Responder Assigned", "Help is en-route", "Arrived at scene"};
        boolean[] completed = {
                true, 
                currentState == State.TRACKING || currentState == State.COMPLETED, 
                false, // In a mock, we don't have "EN_ROUTE" state in the enum yet
                currentState == State.COMPLETED
        };

        for (int i = 0; i < steps.length; i++) {
            View stepView = inflater.inflate(R.layout.item_sos_status, statusTimeline, false);
            TextView title = stepView.findViewById(R.id.tvStatusTitle);
            TextView desc = stepView.findViewById(R.id.tvStatusDesc);
            ImageView dot = stepView.findViewById(R.id.ivStatusDot);
            View line = stepView.findViewById(R.id.vStatusLine);

            title.setText(steps[i]);
            if (i == steps.length - 1) line.setVisibility(View.GONE);

            if (completed[i]) {
                title.setAlpha(1.0f);
                desc.setAlpha(1.0f);
                dot.setAlpha(1.0f);
                dot.setImageResource(R.drawable.ic_check);
                dot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.green_verified)));
                desc.setText(R.string.completed);
            } else if (i > 0 && completed[i-1]) {
                title.setAlpha(1.0f);
                desc.setAlpha(1.0f);
                dot.setAlpha(1.0f);
                dot.setImageResource(R.drawable.ic_clock);
                desc.setText(R.string.in_progress);
                
                Animation pulse = new AlphaAnimation(1.0f, 0.3f);
                pulse.setDuration(1000);
                pulse.setRepeatMode(Animation.REVERSE);
                pulse.setRepeatCount(Animation.INFINITE);
                dot.startAnimation(pulse);
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
            alertManager.stopSiren();
            btnMuteSiren.setText("Unmute Siren");
        } else {
            alertManager.startSiren();
            btnMuteSiren.setText("Mute Siren");
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
        alertManager.stopSiren();
        alertManager.clearNotifications();
        socketManager.off("status_update");
        socketManager.off("agent_location");
        handler.removeCallbacksAndMessages(null);
    }
}
