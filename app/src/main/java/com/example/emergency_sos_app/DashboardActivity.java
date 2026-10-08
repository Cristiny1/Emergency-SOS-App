package com.example.emergency_sos_app;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

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
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Main Dashboard - The active hub for SOS alerts and citizen reporting.
 */
public class DashboardActivity extends BaseActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    private TextView tvDynamicGreeting, tvGlanceBlood, tvGlanceAllergies, tvGlanceContact, tvCurrentAddress, tvDashboardClock, tvGuardianStatus;
    private View vGuardianPulse;
    private Vibrator vibrator;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private DrawerLayout drawerLayout;
    private double currentLat = 11.5564, currentLng = 104.9282;

    private static final long ADDRESS_UPDATE_DEBOUNCE_MS = 1500L;
    private static final double ADDRESS_UPDATE_DISTANCE_METERS = 0.0003d;

    private final Handler sosHoldHandler = new Handler(Looper.getMainLooper());
    private final Handler clockHandler = new Handler(Looper.getMainLooper());
    private boolean isSosHolding = false;
    private Intent pendingIntent;
    private long lastAddressUpdateAt = 0L;
    private double lastGeocodeLat = Double.NaN;
    private double lastGeocodeLng = Double.NaN;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        setupLocationTracking();
        initViews();
        setupActionGrid();
        setupDrawer();
        // setupEdgeToEdge(); // Removed - Handled by setupNavigation in Base
        setupNavigation(R.id.nav_home);

        updateGreeting();
        loadMedicalGlance();
        startPulseAnimation();
        setupSocketStatusIndicator();
        requestLocation();
        startClock();

        // Start Safety Sensor Service
        startService(new Intent(this, SafetySensorService.class));

        // Connect to backend (even if in processing)
        SocketManager.getInstance().connect(null);

        // Hamburger Menu
        findViewById(R.id.btnMenu).setOnClickListener(v -> {
            vibrate(10);
            drawerLayout.openDrawer(GravityCompat.START);
        });

        // Profile Header with Transition
        findViewById(R.id.btnProfileHeader).setOnClickListener(v -> {
            Intent intent = new Intent(this, AccountDetailActivity.class);
            androidx.core.app.ActivityOptionsCompat options = androidx.core.app.ActivityOptionsCompat.makeSceneTransitionAnimation(this, v, "profile_photo");
            startActivity(intent, options.toBundle());
        });

        // Language Toggle
        ImageView flagButton = findViewById(R.id.btn_language_flag);
        if (flagButton != null) {
            flagButton.setImageResource(LanguageManager.getFlagDrawable(this));
            flagButton.setOnClickListener(v -> LanguagePickerDialog.show(this));
        }

        // Chatbot Link
        //findViewById(R.id.btnOpenChatbot).setOnClickListener(v -> startFadeActivity(new Intent(this, ChatbotActivity.class)));

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent == null) return;
        
        if (intent.getBooleanExtra("IMPACT_DETECTED", false)) {
            showImpactConfirmationDialog();
        } else if (intent.getBooleanExtra("WIDGET_SOS_TRIGGER", false)) {
            triggerSOS();
        }
    }

    private void showImpactConfirmationDialog() {
        vibrateEmergency();
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("🚨 Impact Detected")
                .setMessage("A potential crash or fall was detected. Do you need emergency assistance?")
                .setPositiveButton("YES, CALL SOS", (d, w) -> triggerSOS())
                .setNegativeButton("I am OK", null)
                .show();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initViews() {
        tvDynamicGreeting = findViewById(R.id.tvDynamicGreeting);
        tvGlanceBlood = findViewById(R.id.tvGlanceBlood);
        tvGlanceAllergies = findViewById(R.id.tvGlanceAllergies);
        tvGlanceContact = findViewById(R.id.tvGlanceContact);
        tvCurrentAddress = findViewById(R.id.tvCurrentAddress);
        tvDashboardClock = findViewById(R.id.tvDashboardClock);
        tvGuardianStatus = findViewById(R.id.tvGuardianStatus);
        vGuardianPulse = findViewById(R.id.vGuardianPulse);

        final TextView tvSosHint = findViewById(R.id.tvSosHint);
        final View btnSos = findViewById(R.id.btnSos);
        final CircularProgressIndicator progressIndicator = findViewById(R.id.sosProgressIndicator);

        btnSos.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    isSosHolding = true;
                    vibrate(50);
                    v.animate().scaleX(1.1f).scaleY(1.1f).setDuration(3000).start();
                    if (tvSosHint != null) tvSosHint.setText(R.string.keep_holding);

                    if (progressIndicator != null) {
                        progressIndicator.setVisibility(View.VISIBLE);
                        progressIndicator.setProgress(0);
                        
                        // Professional smooth progress animation
                        ValueAnimator animator = ValueAnimator.ofInt(0, 100);
                        animator.setDuration(3000);
                        animator.addUpdateListener(animation -> {
                            if (isSosHolding) {
                                progressIndicator.setProgress((int) animation.getAnimatedValue());
                            } else {
                                animator.cancel();
                            }
                        });
                        animator.start();
                    }

                    sosHoldHandler.postDelayed(() -> {
                        if (isSosHolding) {
                            vibrateEmergency();
                            triggerSOS();
                            resetSosButton(v, tvSosHint, progressIndicator);
                        }
                    }, 3000);
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    isSosHolding = false;
                    sosHoldHandler.removeCallbacksAndMessages(null);
                    resetSosButton(v, tvSosHint, progressIndicator);
                    return true;
            }
            return false;
        });
    }

    private void resetSosButton(View v, TextView hint, CircularProgressIndicator progress) {
        v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(300).setInterpolator(new OvershootInterpolator()).start();
        if (hint != null) hint.setText(R.string.hold_to_request_assistance);
        if (progress != null) {
            progress.setVisibility(View.INVISIBLE);
            progress.setProgress(0);
        }
    }

    private void setupDrawer() {
        drawerLayout = findViewById(R.id.drawerLayout);
        NavigationView navigationView = findViewById(R.id.navigationView);

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            vibrate(20);
            
            // Visual feedback
            item.setChecked(true);

            if (id == R.id.nav_drawer_history) {
                pendingIntent = new Intent(this, SosHistoryActivity.class);
            } else if (id == R.id.nav_drawer_contacts) {
                pendingIntent = new Intent(this, EmergencyDirectoryActivity.class);
            } else if (id == R.id.nav_drawer_account) {
                pendingIntent = new Intent(this, AccountDetailActivity.class);
            } else if (id == R.id.nav_drawer_medical) {
                pendingIntent = new Intent(this, ProfileActivity.class);
            } else if (id == R.id.nav_drawer_settings) {
                pendingIntent = new Intent(this, SettingsActivity.class);
            } else if (id == R.id.nav_drawer_help) {
                pendingIntent = new Intent(this, SettingsDetailActivity.class);
                pendingIntent.putExtra("TYPE", "help");
            } else if (id == R.id.nav_drawer_logout) {
                pendingIntent = null;
                performLogout();
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerClosed(View drawerView) {
                if (pendingIntent != null) {
                    startFadeActivity(pendingIntent);
                    pendingIntent = null;
                }
            }
        });

        updateDrawerHeader();
    }

    private void updateDrawerHeader() {
        NavigationView navigationView = findViewById(R.id.navigationView);
        if (navigationView == null) return;
        
        View headerView = navigationView.getHeaderView(0);
        TextView tvName = headerView.findViewById(R.id.tvHeaderName);
        ImageView ivHeaderProfile = headerView.findViewById(R.id.ivHeaderProfile);

        SharedPreferences prefs = getSharedPreferences("sos_profile_prefs", MODE_PRIVATE);
        String savedName = prefs.getString("name", getString(R.string.default_user_name));
        
        if (tvName != null) tvName.setText(savedName);
        if (ivHeaderProfile != null) {
            String photoPath = prefs.getString("profile_photo_path", null);
            if (photoPath != null) {
                java.io.File file = new java.io.File(photoPath);
                if (file.exists()) {
                    ivHeaderProfile.setImageURI(Uri.fromFile(file));
                } else {
                    ivHeaderProfile.setImageResource(R.drawable.ic_personal);
                }
            } else {
                ivHeaderProfile.setImageResource(R.drawable.ic_personal);
            }

            ivHeaderProfile.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                pendingIntent = new Intent(this, ProfileActivity.class);
            });
        }

        // Also update Main Header Profile
        ImageView ivMainProfile = findViewById(R.id.btnProfileHeader);
        if (ivMainProfile != null) {
            String photoPath = prefs.getString("profile_photo_path", null);
            if (photoPath != null) {
                java.io.File file = new java.io.File(photoPath);
                if (file.exists()) {
                    ivMainProfile.setImageURI(Uri.fromFile(file));
                } else {
                    ivMainProfile.setImageResource(R.drawable.ic_personal);
                }
            } else {
                ivMainProfile.setImageResource(R.drawable.ic_personal);
            }
        }
    }

    private void startClock() {
        Runnable clockRunnable = new Runnable() {
            @Override
            public void run() {
                if (isFinishing() || isDestroyed()) return;
                
                SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMM dd, yyyy\nhh:mm:ss a", Locale.getDefault());
                if (tvDashboardClock != null) {
                    tvDashboardClock.setText(sdf.format(new Date()));
                }
                clockHandler.postDelayed(this, 1000);
            }
        };
        clockHandler.post(clockRunnable);
    }

    private void performLogout() {
        getSharedPreferences("sos_profile_prefs", MODE_PRIVATE).edit()
                .putBoolean("remember", false)
                .apply();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupLocationTracking() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (android.location.Location location : locationResult.getLocations()) {
                    currentLat = location.getLatitude();
                    currentLng = location.getLongitude();
                    
                    float accuracy = location.getAccuracy();
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

                    if (accuracy > 50) {
                        tvCurrentAddress.setText(getString(R.string.low_gps_accuracy, accuracy));
                        tvCurrentAddress.setTextColor(color);
                    } else {
                        tvCurrentAddress.setTextColor(getColor(R.color.text_dark));
                        if (shouldRefreshAddress(currentLat, currentLng)) {
                            updateAddress(currentLat, currentLng);
                        }

                        // Show authoritative GPS Status
                        String statusMsg = getString(R.string.gps_status_label, accuracyStatus, accuracy);
                        Log.d("GPS", statusMsg);

                        // Optionally show on UI if needed, for now address bar + Log
                        String current = tvCurrentAddress.getText().toString();
                        if (!current.contains("(")) {
                            tvCurrentAddress.setText(current + " (" + accuracyStatus + ")");
                        }
                    }
                }
            }
        };
    }

    private void requestLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            
            // Check if Location Services are enabled
            android.location.LocationManager lm = (android.location.LocationManager) getSystemService(Context.LOCATION_SERVICE);
            boolean gpsEnabled = false;
            try { gpsEnabled = lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER); } catch(Exception ignored) {}
            
            if (!gpsEnabled) {
                tvCurrentAddress.setText(R.string.please_turn_on_gps);
                return;
            }

            LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                    .setMinUpdateDistanceMeters(5)
                    .build();

            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());

            // Also get last known for immediate load
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    currentLat = location.getLatitude();
                    currentLng = location.getLongitude();
                    updateAddress(currentLat, currentLng);
                } else {
                    tvCurrentAddress.setText(R.string.searching_gps);
                }
            });
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestLocation();
            } else if (tvCurrentAddress != null) {
                tvCurrentAddress.setText(R.string.location_locked);
            }
        }
    }

    private boolean shouldRefreshAddress(double lat, double lng) {
        long now = System.currentTimeMillis();
        if (Double.isNaN(lastGeocodeLat) || Double.isNaN(lastGeocodeLng)) {
            return true;
        }

        double distanceDelta = Math.hypot(lat - lastGeocodeLat, lng - lastGeocodeLng);
        return distanceDelta >= ADDRESS_UPDATE_DISTANCE_METERS || (now - lastAddressUpdateAt) >= ADDRESS_UPDATE_DEBOUNCE_MS;
    }

    private void updateAddress(double lat, double lng) {
        lastGeocodeLat = lat;
        lastGeocodeLng = lng;
        lastAddressUpdateAt = System.currentTimeMillis();

        // Update Proximity Info
        ProximityManager.Facility nearestPolice = ProximityManager.getNearest(lat, lng, "POLICE");
        ProximityManager.Facility nearestHospital = ProximityManager.getNearest(lat, lng, "MEDICAL");

        runOnUiThread(() -> {
            if (nearestPolice != null) {
                ((TextView)findViewById(R.id.tvNearestPolice)).setText(nearestPolice.name);
                ((TextView)findViewById(R.id.tvPoliceEta)).setText(getString(R.string.eta_minutes, nearestPolice.travelTimeMins));
            }
            if (nearestHospital != null) {
                ((TextView)findViewById(R.id.tvNearestHospital)).setText(nearestHospital.name);
                ((TextView)findViewById(R.id.tvHospitalEta)).setText(getString(R.string.eta_minutes, nearestHospital.travelTimeMins));
            }
        });

        new Thread(() -> {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            String resolvedAddress = null;
            try {
                List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address addr = addresses.get(0);
                    String fullAddress = addr.getAddressLine(0);

                    if (fullAddress != null) {
                        resolvedAddress = fullAddress;
                        if (!resolvedAddress.toLowerCase().contains("cambodia")) {
                            resolvedAddress += ", Cambodia";
                        }
                    }
                }
            } catch (Exception e) {
                resolvedAddress = String.format(Locale.getDefault(), "Lat: %.4f, Lng: %.4f", lat, lng);
            }

            final String finalAddr = resolvedAddress;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed() || tvCurrentAddress == null) return;
                tvCurrentAddress.setText(finalAddr != null ? finalAddr : getString(R.string.searching_gps));
            });
        }).start();
    }

    private void vibrateEmergency() {
        if (vibrator != null) {
            vibrator.vibrate(android.os.VibrationEffect.createWaveform(new long[]{0, 100, 50, 150}, -1));
        }
    }

    private void startPulseAnimation() {
        View ring1 = findViewById(R.id.pulseRing1);
        View ring2 = findViewById(R.id.pulseRing2);
        if (ring1 != null) animateRing(ring1, 0);
        if (ring2 != null) animateRing(ring2, 800);
    }

    private void animateRing(View view, long delay) {
        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(view,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.8f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.8f),
                PropertyValuesHolder.ofFloat(View.ALPHA, 0.4f, 0.0f)
        );
        animator.setDuration(2200);
        animator.setStartDelay(delay);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.start();
    }

    private void loadMedicalGlance() {
        SharedPreferences prefs = getSharedPreferences("sos_profile_prefs", MODE_PRIVATE);
        tvGlanceBlood.setText(prefs.getString("blood_group", "O+"));
        tvGlanceAllergies.setText(prefs.getString("allergies", "None"));
        String contact = prefs.getString("contacts", "No Contact");
        if (contact.contains(",")) contact = contact.split(",")[0].trim();
        tvGlanceContact.setText(contact);
    }

    private void setupSocketStatusIndicator() {
        SocketManager socketManager = SocketManager.getInstance();

        socketManager.on(io.socket.client.Socket.EVENT_CONNECT, args -> runOnUiThread(() -> {
            if (tvGuardianStatus == null || isFinishing() || isDestroyed()) return;
            tvGuardianStatus.setText(R.string.system_connected);
            tvGuardianStatus.setTextColor(getColor(R.color.green_verified));
            if (vGuardianPulse != null) {
                vGuardianPulse.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.green_verified)));
                startGuardianPulse();
            }
        }));

        socketManager.on(io.socket.client.Socket.EVENT_CONNECT_ERROR, args -> runOnUiThread(() -> {
            if (tvGuardianStatus == null || isFinishing() || isDestroyed()) return;
            tvGuardianStatus.setText(R.string.system_offline);
            tvGuardianStatus.setTextColor(getColor(R.color.sos_red));
            if (vGuardianPulse != null) {
                vGuardianPulse.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.sos_red)));
                vGuardianPulse.clearAnimation();
            }
        }));

        // Initial state
        if (socketManager.isConnected()) {
            tvGuardianStatus.setText(R.string.system_connected);
            startGuardianPulse();
        } else {
            tvGuardianStatus.setText(R.string.system_offline);
        }
    }

    private void startGuardianPulse() {
        if (vGuardianPulse == null) return;
        ObjectAnimator pulse = ObjectAnimator.ofPropertyValuesHolder(vGuardianPulse,
                PropertyValuesHolder.ofFloat(View.ALPHA, 1.0f, 0.4f)
        );
        pulse.setDuration(1000);
        pulse.setRepeatMode(ValueAnimator.REVERSE);
        pulse.setRepeatCount(ValueAnimator.INFINITE);
        pulse.start();
    }

    private void updateGreeting() {
        SharedPreferences prefs = getSharedPreferences("sos_profile_prefs", MODE_PRIVATE);
        String name = prefs.getString("name", "User");
        tvDynamicGreeting.setText(getString(R.string.dashboard_greeting, name));
    }

    // language picker
//    @Override
//    protected void onResume() {
//        super.onResume();
//        loadMedicalGlance();
//        updateGreeting();
//        checkReportStatus();
//        updateLanguageFlag();
//    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sosHoldHandler.removeCallbacksAndMessages(null);
    }

    private void checkReportStatus() {
        View card = findViewById(R.id.cardReportStatus);
        if (card == null) return;

        SharedPreferences prefs = getSharedPreferences("sos_profile_prefs", MODE_PRIVATE);
        boolean isInProgress = prefs.getBoolean("report_in_progress", false);

        if (isInProgress) {
            card.setVisibility(View.VISIBLE);
            TextView tvTitle = findViewById(R.id.tvReportTitle);
            TextView tvDetail = findViewById(R.id.tvReportDetail);
            String category = prefs.getString("active_report_category", "Issue");

            tvTitle.setText(getString(R.string.report_in_process, category));
            tvDetail.setText(R.string.report_status_pending);

            findViewById(R.id.btnClearReport).setOnClickListener(v -> {
                prefs.edit().putBoolean("report_in_progress", false).apply();
                card.setVisibility(View.GONE);
                Toast.makeText(this, R.string.report_cleared_toast, Toast.LENGTH_SHORT).show();
            });
        } else {
            card.setVisibility(View.GONE);
        }
    }

    private void updateLanguageFlag() {
        ImageView flagButton = findViewById(R.id.btn_language_flag);
        if (flagButton != null) {
            flagButton.setImageResource(LanguageManager.getFlagDrawable(this));
        }
    }

    private void setupActionGrid() {
        setupCardInteraction(findViewById(R.id.cardFamily), this::triggerFamilyAction);
        setupCardInteraction(findViewById(R.id.cardAmbulance), () -> showCallConfirmation("Ambulance", "MEDICAL"));
        setupCardInteraction(findViewById(R.id.cardPolice), () -> showCallConfirmation("Police", "POLICE"));
        setupCardInteraction(findViewById(R.id.cardReportDanger), () -> startFadeActivity(new Intent(this, CitizenReportActivity.class)));
        setupCardInteraction(findViewById(R.id.cardCalendar), () -> startFadeActivity(new Intent(this, CalendarActivity.class)));
        
        // --- NEW: INTERACTIVE PROXIMITY CARDS ---
        View layoutHelp = findViewById(R.id.layoutNearestHelp);
        if (layoutHelp != null) {
            layoutHelp.setOnClickListener(v -> {
                Intent intent = new Intent(this, EmergencyDirectoryActivity.class);
                startFadeActivity(intent);
            });
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupCardInteraction(View view, Runnable action) {
        if (view == null) return;
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // Instant touch response
                    v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(60).start();
                    vibrate(10);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    // Rapid recovery
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).setInterpolator(new OvershootInterpolator()).start();
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        action.run();
                        v.performClick();
                    }
                    break;
            }
            return true;
        });
    }

    private void showCallConfirmation(String name, String filter) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.emergency_call_title)
                .setMessage(getString(R.string.emergency_call_message, name))
                .setPositiveButton(R.string.call_now, (d, w) -> {
                    Intent intent = new Intent(this, EmergencyDirectoryActivity.class);
                    intent.putExtra("FILTER_TYPE", filter);
                    startFadeActivity(intent);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void triggerSOS() {
        try {
            // High Priority SOS Trigger
            vibrateEmergency();

            com.google.android.material.bottomsheet.BottomSheetDialog dialog = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
            @SuppressLint("InflateParams") View dialogView = android.view.LayoutInflater.from(this).inflate(R.layout.dialog_sos_type, null);
            dialog.setContentView(dialogView);

            dialogView.findViewById(R.id.typeMedical).setOnClickListener(v -> {
                dialog.dismiss();
                emitSos("Medical");
            });
            dialogView.findViewById(R.id.typeFire).setOnClickListener(v -> {
                dialog.dismiss();
                emitSos("Fire");
            });
            dialogView.findViewById(R.id.typePolice).setOnClickListener(v -> {
                dialog.dismiss();
                emitSos("Police");
            });
            dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
            dialog.show();
        } catch (Exception e) {
            Log.e("Dashboard", "Error showing SOS dialog", e);
            emitSos("Medical"); // Fallback
        }
    }

    private void emitSos(String type) {
        try {
            SharedPreferences prefs = getSharedPreferences("sos_profile_prefs", MODE_PRIVATE);
            String name = prefs.getString("name", "Unknown User");

            // Generate a unique ID for this session
            String sosId = "SOS_" + System.currentTimeMillis();

            // Create Domain Event
            SosEvent event = new SosEvent(sosId, name, type, currentLat, currentLng, 0);

            com.google.android.material.switchmaterial.SwitchMaterial switchSilent = findViewById(R.id.switchSilentMode);
            final boolean isSilent = switchSilent != null && switchSilent.isChecked();

            // Persist before dispatch so a process death cannot lose the active incident.
            prefs.edit().putBoolean("is_sos_active", true)
                       .putString("active_sos_id", sosId)
                       .putString("active_sos_type", type)
                       .putBoolean("active_sos_silent", isSilent)
                       .apply();

            // Use Repository to trigger SOS
            RepositoryProvider.getSosRepository(this).createSos(event, new SosRepository.SosCallback() {
                @Override
                public void onStatusChanged(SosStatus status) {
                    Log.d("SOS", "Repository Status changed to: " + status);
                    if (status == SosStatus.PENDING) {
                        runOnUiThread(() -> {
                            String msg = isSilent ? getString(R.string.silent_signal_broadcasting) : getString(R.string.emergency_signal_broadcasting);
                            Toast.makeText(DashboardActivity.this, msg, Toast.LENGTH_SHORT).show();
                        });
                        
                        // Notify Family Circle (Simulation)
                        notifyFamilyCircle(type, sosId);
                    }
                }

                @Override
                public void onError(String message) {
                    prefs.edit().putBoolean("is_sos_active", false)
                               .remove("active_sos_id")
                               .remove("active_sos_type")
                               .remove("active_sos_silent")
                               .apply();
                    runOnUiThread(() -> Toast.makeText(DashboardActivity.this, getString(R.string.sos_failed_msg, message), Toast.LENGTH_LONG).show());
                }
            });

            // Save to Local History
            String address = "Unknown Location";
            if (tvCurrentAddress != null && tvCurrentAddress.getText() != null) {
                address = tvCurrentAddress.getText().toString();
            }
            HistoryManager.saveEvent(this, type, address);

            // Move to Workflow Screen
            startWorkflow(type, sosId, isSilent, currentLat, currentLng);
        } catch (Exception e) {
            Log.e("SOS", "Error emitting SOS", e);
            Toast.makeText(this, R.string.emergency_signal_error, Toast.LENGTH_LONG).show();
        }
    }

    private void notifyFamilyCircle(String type, String sosId) {
        List<FamilyManager.FamilyMember> members = FamilyManager.getMembers(this);
        if (members.isEmpty()) return;
        
        String safetyLink = "https://sos-cambodia.com/track/" + sosId;
        Log.d("SOS", "Sending Alert to Family Circle: " + safetyLink);
        
        String message = String.format(Locale.getDefault(), 
            "EMERGENCY: %s triggered an SOS. Track live: %s", 
            getSharedPreferences("sos_profile_prefs", MODE_PRIVATE).getString("name", "User"),
            safetyLink);
            
        // Simulation of SMS broadcast
        Toast.makeText(this, R.string.family_notified_sms, Toast.LENGTH_SHORT).show();
    }

    private void startWorkflow(String type, String sosId, boolean isSilent, double latitude, double longitude) {
        Log.d("Dashboard", "Launching Workflow for ID: " + sosId);
        Intent intent = new Intent(this, SosWorkflowActivity.class);
        intent.putExtra("INCIDENT_TYPE", type);
        intent.putExtra("SOS_ID", sosId);
        intent.putExtra("IS_SILENT", isSilent);
        intent.putExtra("USER_LAT", latitude);
        intent.putExtra("USER_LNG", longitude);
        startFadeActivity(intent);
    }

    private void setupEdgeToEdge() {
        View root = findViewById(R.id.drawerLayout);
        View topBar = findViewById(R.id.topBar);
        View topBarContent = findViewById(R.id.topBarContent);
        View bottomNav = findViewById(R.id.bottomNavigation);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            int navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;

            // Apply padding to the top bar CONTENT to avoid status bar overlap
            // while letting the MaterialCardView background flow to the top.
            if (topBarContent != null) {
                int internalPadding = (int) (12 * getResources().getDisplayMetrics().density);
                topBarContent.setPadding(topBarContent.getPaddingLeft(), statusBarHeight + internalPadding, 
                                        topBarContent.getPaddingRight(), internalPadding);
            }

            // Apply margin to bottom navigation to avoid system nav bar overlap
            if (bottomNav != null) {
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) bottomNav.getLayoutParams();
                lp.bottomMargin = (int) (16 * getResources().getDisplayMetrics().density) + navBarHeight; 
                bottomNav.setLayoutParams(lp);
            }

            return WindowInsetsCompat.CONSUMED;
        });
    }


    private void triggerFamilyAction() {
        startFadeActivity(new Intent(this, FamilyActivity.class));
    }

    // Dashboard State Update
    @Override
    protected void onResume() {
        super.onResume();
        // The BaseActivity handles the fade-in. We just update the state.
        loadMedicalGlance();
        updateGreeting();
        updateDrawerHeader();
        checkReportStatus();
        requestLocation();
        
        ImageView flagButton = findViewById(R.id.btn_language_flag);
        if (flagButton != null) {
            flagButton.setImageResource(LanguageManager.getFlagDrawable(this));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}
