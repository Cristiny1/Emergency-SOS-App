package com.example.emergency_sos_app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TrackingActivity extends BaseActivity {

    private WebView mapWebView;
    private boolean isMapLoaded = false;
    private double userLat = 13.3633, userLng = 103.8564;

    private BottomSheetBehavior<View> bottomSheetBehavior;
    private TextView tvStatus, tvEta, tvAgentName, tvAgentDistance;
    private LinearLayout layoutResponder;
    
    private FirebaseLocationManager firebaseManager;
    private FusedLocationProviderClient locationClient;
    private LocationCallback locationCallback;
    private String requestId;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private SosAlertManager alertManager;
    private boolean isSimulating = false;
    private boolean isAssigned = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tracking_map);

        requestId = getIntent().getStringExtra("REQUEST_ID");
        if (requestId == null) requestId = "demo_request";

        initViews(savedInstanceState);
        setupWebView();
        
        alertManager = new SosAlertManager(this);
        try {
            firebaseManager = new FirebaseLocationManager(requestId);
        } catch (Exception e) {
            Log.e("Tracking", "Firebase fail: " + e.getMessage());
        }
        
        locationClient = LocationServices.getFusedLocationProviderClient(this);
        requestNecessaryPermissions();
        
        handler.postDelayed(this::checkAndStartSimulation, 5000);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        mapWebView = findViewById(R.id.trackingMapView);
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
        mapWebView.evaluateJavascript("setLocation(" + userLat + "," + userLng + ", 'You')", null);
    }

    private void requestNecessaryPermissions() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), 101);
        } else {
            startTracking();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) startTracking();
    }

    private void initViews(Bundle savedInstanceState) {
        tvStatus = findViewById(R.id.tvTrackingStatus);
        tvEta = findViewById(R.id.tvTrackingEta);
        tvAgentName = findViewById(R.id.tvAgentName);
        tvAgentDistance = findViewById(R.id.tvAgentDistance);
        layoutResponder = findViewById(R.id.layoutResponder);

        View bottomSheet = findViewById(R.id.trackingBottomSheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnCancelTracking).setOnClickListener(v -> finish());
        findViewById(R.id.btnCallAgent).setOnClickListener(v -> callAgent());
        
        findViewById(R.id.btnToggle3D).setVisibility(View.GONE);
        findViewById(R.id.btnToggleLayers).setOnClickListener(v -> Toast.makeText(this, R.string.tracking_map_layers_message, Toast.LENGTH_SHORT).show());
    }

    private void checkAndStartSimulation() {
        if (!isAssigned && !isSimulating) {
            isSimulating = true;
            updateUIForStatus("ASSIGNED", "Rescue Unit SIM-01");
            
            double startLat = userLat + 0.008;
            double startLng = userLng + 0.008;
            
            handler.postDelayed(() -> updateAgentOnMap(startLat, startLng), 1000);
            handler.postDelayed(() -> updateAgentOnMap(userLat + 0.004, userLng + 0.004), 4000);
            handler.postDelayed(() -> updateAgentOnMap(userLat + 0.001, userLng + 0.001), 8000);
            handler.postDelayed(() -> updateUIForStatus("ARRIVED", "Rescue Unit SIM-01"), 10000);
        }
    }

    private void updateAgentOnMap(double lat, double lng) {
        if (!isMapLoaded) return;
        mapWebView.evaluateJavascript("setResponder(" + lat + "," + lng + ", 'Rescue Unit')", null);
        mapWebView.evaluateJavascript("updateRoute(" + lat + "," + lng + "," + userLat + "," + userLng + ")", null);
        mapWebView.evaluateJavascript("fitToMarkers(" + userLat + "," + userLng + "," + lat + "," + lng + ")", null);
        
        float[] results = new float[1];
        android.location.Location.distanceBetween(lat, lng, userLat, userLng, results);
        updateDistanceText(results[0]);
    }

    private void updateDistanceText(float distance) {
        tvAgentDistance.setText(getString(R.string.tracking_distance_km, distance / 1000));
        int mins = (int) (distance / 600);
        if (mins < 1) tvEta.setText(R.string.tracking_arriving_now);
        else tvEta.setText(getString(R.string.tracking_arrival_in_minutes, mins));
    }

    private void startTracking() {
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
                        updateMapLocation();
                        
                        if (firebaseManager != null) {
                            Map<String, Object> data = new HashMap<>();
                            data.put("userLat", userLat);
                            data.put("userLng", userLng);
                            firebaseManager.updateRequest(data);
                        }
                    }
                }
            };

            locationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());

            // Initial immediate update
            locationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    userLat = location.getLatitude();
                    userLng = location.getLongitude();
                    updateMapLocation();
                }
            });
        }

        if (firebaseManager != null) {
            firebaseManager.startListening(new FirebaseLocationManager.LocationUpdateListener() {
                @Override
                public void onLocationUpdate(double lat, double lng, float bearing) {
                    updateAgentOnMap(lat, lng);
                }
                @Override
                public void onStatusUpdate(String status, String agentName) {
                    updateUIForStatus(status, agentName);
                }
            });
        }
    }

    private void updateUIForStatus(String status, String agentName) {
        isAssigned = true;
        handler.removeCallbacksAndMessages(null);
        runOnUiThread(() -> {
            if ("ASSIGNED".equals(status) || "EN_ROUTE".equals(status)) {
                tvStatus.setText(R.string.tracking_rescue_on_the_way);
                layoutResponder.setVisibility(View.VISIBLE);
                tvAgentName.setText(agentName != null ? agentName : getString(R.string.tracking_default_agent_name));
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            } else if ("ARRIVED".equals(status)) {
                tvStatus.setText(R.string.tracking_help_arrived);
                tvStatus.setTextColor(getColor(R.color.green_verified));
                tvEta.setText(R.string.tracking_arrival_confirmed);
            }
        });
    }

    private void callAgent() {
        startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:012345678")));
    }

    @Override protected void onResume() { super.onResume(); }

    @Override
    protected void onPause() {
        super.onPause();
        if (locationClient != null && locationCallback != null) {
            locationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (alertManager != null) alertManager.stopSiren();
        handler.removeCallbacksAndMessages(null);
    }
}
