package com.example.emergency_sos_app;

import android.annotation.SuppressLint;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class EmergencyDirectoryActivity extends BaseActivity {

    private WebView mapWebView;
    private boolean isMapLoaded = false;
    private final List<Facility> facilities = new ArrayList<>();
    private FusedLocationProviderClient fusedLocationClient;
    private Location userLocation;
    private LinearLayout directoryContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_directory);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        directoryContainer = findViewById(R.id.directoryContainer);

        setupEdgeToEdge();
        setupWebView();
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        loadData();
        requestUserLocation();
    }

    private void requestUserLocation() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    userLocation = location;
                    refreshDirectory();
                } else {
                    // Fallback to Siem Reap center if no GPS
                    userLocation = new Location("");
                    userLocation.setLatitude(13.3633);
                    userLocation.setLongitude(103.8564);
                    refreshDirectory();
                }
            });
        } else {
            refreshDirectory(); // Show default sorted if no permission
        }
    }

    private void refreshDirectory() {
        if (userLocation != null) {
            for (Facility f : facilities) {
                float[] results = new float[1];
                Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(), 
                                       f.locationLat, f.locationLng, results);
                f.distance = results[0] / 1000f; // in km
                f.travelTime = (int) (f.distance * 2.5); // Rough estimate: 2.5 min per km in city
            }
            Collections.sort(facilities, (f1, f2) -> Float.compare(f1.distance, f2.distance));
        }

        directoryContainer.removeAllViews();
        String filter = getIntent().getStringExtra("FILTER_TYPE");
        for (Facility f : facilities) {
            if (filter == null || filter.isEmpty() || f.category.equals(filter)) {
                addFacilityToUI(directoryContainer, f);
            }
        }
        showMarkersOnMap();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        mapWebView = findViewById(R.id.mapWebView);
        WebSettings settings = mapWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        mapWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                isMapLoaded = true;
                showMarkersOnMap();
            }
        });
        mapWebView.loadUrl("file:///android_asset/leaflet_map.html");
    }

    private void setupEdgeToEdge() {
        View toolbarContainer = findViewById(R.id.toolbarContainer);
        View bottomSheet = findViewById(R.id.explorerBottomSheet);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            int bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;

            if (toolbarContainer != null) {
                int padding = (int)(16 * getResources().getDisplayMetrics().density);
                toolbarContainer.setPadding(padding, top + padding, padding, padding);
            }
            if (bottomSheet != null) {
                bottomSheet.setPadding(0, 0, 0, bottom);
            }
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void showMarkersOnMap() {
        if (!isMapLoaded) return;
        mapWebView.evaluateJavascript("clearMarkers()", null);
        
        // Show User Location on Map
        if (userLocation != null) {
            mapWebView.evaluateJavascript("setLocation(" + userLocation.getLatitude() + "," + userLocation.getLongitude() + ", 'You')", null);
        }

        String filter = getIntent().getStringExtra("FILTER_TYPE");
        for (Facility f : facilities) {
            if (filter == null || filter.isEmpty() || f.category.equals(filter)) {
                mapWebView.evaluateJavascript("addMarker(" + f.locationLat + "," + f.locationLng + ",'" + f.name + "','" + f.category + "')", null);
            }
        }
        
        // Zoom to fit all
        mapWebView.evaluateJavascript("fitAllMarkers()", null);
    }

    private void loadData() {
        facilities.clear();
        facilities.add(new Facility("Siem Reap Provincial Police", "POLICE", "063760259", 13.3633, 103.8567));
        facilities.add(new Facility("Siem Reap Fire Station", "FIRE", "012891100", 13.3556, 103.8544));
        facilities.add(new Facility("Siem Reap Referral Hospital", "MEDICAL", "063761111", 13.3622, 103.8599));
        facilities.add(new Facility("Angkor Hospital for Children", "MEDICAL", "063963409", 13.3639, 103.8547));
        facilities.add(new Facility("Jayavarman VII Hospital", "MEDICAL", "063963409", 13.3761, 103.8592));
    }

    private void addFacilityToUI(LinearLayout container, Facility f) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_directory_facility, container, false);
        ((TextView)view.findViewById(R.id.tvFacilityName)).setText(f.name);
        ((TextView)view.findViewById(R.id.tvFacilityType)).setText(f.category);

        if (f.distance > 0) {
            ((TextView)view.findViewById(R.id.tvDistance)).setText(String.format(Locale.getDefault(), "%.1f km", f.distance));
            ((TextView)view.findViewById(R.id.tvTravelTime)).setText(String.format(Locale.getDefault(), "%d mins", f.travelTime));
        }

        view.findViewById(R.id.btnCallFacility).setOnClickListener(v -> {
            startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + f.phone)));
        });

        view.findViewById(R.id.btnNavFacility).setOnClickListener(v -> {
            if (isMapLoaded) {
                mapWebView.evaluateJavascript("zoomTo(" + f.locationLat + "," + f.locationLng + ")", null);
            }
        });

        container.addView(view);
    }

    private static class Facility {
        String name, category, phone;
        double locationLat, locationLng;
        float distance;
        int travelTime;
        Facility(String n, String c, String p, double lat, double lng) {
            name = n; category = c; phone = p; locationLat = lat; locationLng = lng;
        }
    }
}
