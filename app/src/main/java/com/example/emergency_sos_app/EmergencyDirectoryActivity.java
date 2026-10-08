package com.example.emergency_sos_app;

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

import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.List;
import java.util.Locale;

public class EmergencyDirectoryActivity extends BaseActivity {

    private WebView mapWebView;
    private boolean isMapLoaded = false;

    private FusedLocationProviderClient fusedLocationClient;
    private Location userLocation;

    private LinearLayout directoryContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_emergency_directory);

        // Initialize location
        fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(this);

        // Initialize directory
        directoryContainer =
                findViewById(R.id.directoryContainer);

        // Setup system UI
        setupEdgeToEdge();

        // Setup map
        setupWebView();

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Get user's location
        requestUserLocation();
    }


    // =========================================================
    // LOCATION
    // =========================================================

    private void requestUserLocation() {

        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient
                    .getLastLocation()
                    .addOnSuccessListener(this, location -> {

                        if (location != null) {

                            userLocation = location;

                        } else {

                            // Fallback to Siem Reap
                            userLocation = new Location("");

                            userLocation.setLatitude(13.3633);
                            userLocation.setLongitude(103.8564);
                        }

                        refreshDirectory();
                    });

        } else {

            // Fallback location
            userLocation = new Location("");

            userLocation.setLatitude(13.3633);
            userLocation.setLongitude(103.8564);

            refreshDirectory();
        }
    }


    // =========================================================
    // DIRECTORY
    // =========================================================

    private void refreshDirectory() {

        if (directoryContainer == null) {
            return;
        }

        directoryContainer.removeAllViews();

        if (userLocation == null) {
            return;
        }

        String filter =
                getIntent().getStringExtra("FILTER_TYPE");

        List<ProximityManager.Facility> results =
                ProximityManager.getNearestFacilities(
                        userLocation.getLatitude(),
                        userLocation.getLongitude(),
                        filter
                );

        // Add facilities to scrollable list
        for (ProximityManager.Facility facility : results) {

            addFacilityToUI(
                    directoryContainer,
                    facility
            );
        }

        // Add markers to map
        showMarkersOnMap(results);
    }


    // =========================================================
    // MAP MARKERS
    // =========================================================

    private void showMarkersOnMap(
            List<ProximityManager.Facility> results
    ) {

        if (!isMapLoaded) {
            return;
        }

        // Clear old markers
        mapWebView.evaluateJavascript(
                "clearMarkers();",
                null
        );

        // User location
        if (userLocation != null) {

            String userMarker =
                    "setLocation("
                            + userLocation.getLatitude()
                            + ","
                            + userLocation.getLongitude()
                            + ",'You');";

            mapWebView.evaluateJavascript(
                    userMarker,
                    null
            );
        }

        // Facility markers
        for (ProximityManager.Facility f : results) {

            String marker =
                    "addMarker("
                            + f.lat
                            + ","
                            + f.lng
                            + ",'"
                            + escapeJavaScript(f.name)
                            + "','"
                            + escapeJavaScript(f.category)
                            + "');";

            mapWebView.evaluateJavascript(
                    marker,
                    null
            );
        }

        // Fit map to all markers
        mapWebView.evaluateJavascript(
                "fitAllMarkers();",
                null
        );
    }


    // =========================================================
    // WEBVIEW MAP
    // =========================================================

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {

        mapWebView =
                findViewById(R.id.mapWebView);

        WebSettings settings =
                mapWebView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        mapWebView.setWebViewClient(
                new WebViewClient() {

                    @Override
                    public void onPageFinished(
                            WebView view,
                            String url
                    ) {

                        isMapLoaded = true;

                        refreshDirectory();
                    }
                }
        );

        mapWebView.loadUrl(
                "file:///android_asset/leaflet_map.html"
        );
    }


    // =========================================================
    // EDGE TO EDGE
    // =========================================================

    private void setupEdgeToEdge() {

        View toolbarContainer =
                findViewById(R.id.toolbarContainer);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(android.R.id.content),
                (v, insets) -> {

                    int top =
                            insets.getInsets(
                                    WindowInsetsCompat.Type.statusBars()
                            ).top;

                    if (toolbarContainer != null) {

                        int padding =
                                (int) (
                                        16 * getResources()
                                                .getDisplayMetrics()
                                                .density
                                );

                        toolbarContainer.setPadding(
                                padding,
                                top + padding,
                                padding,
                                padding
                        );
                    }

                    return insets;
                }
        );
    }


    // =========================================================
    // FACILITY CARD
    // =========================================================

    private void addFacilityToUI(
            LinearLayout container,
            ProximityManager.Facility f
    ) {

        View view =
                LayoutInflater
                        .from(this)
                        .inflate(
                                R.layout.item_directory_facility,
                                container,
                                false
                        );

        TextView facilityName =
                view.findViewById(
                        R.id.tvFacilityName
                );

        TextView facilityType =
                view.findViewById(
                        R.id.tvFacilityType
                );

        TextView distance =
                view.findViewById(
                        R.id.tvDistance
                );

        TextView travelTime =
                view.findViewById(
                        R.id.tvTravelTime
                );

        facilityName.setText(f.name);

        facilityType.setText(f.category);

        if (f.distanceKm > 0) {

            distance.setText(
                    String.format(
                            Locale.getDefault(),
                            "%.1f km",
                            f.distanceKm
                    )
            );

            travelTime.setText(
                    String.format(
                            Locale.getDefault(),
                            "%d mins",
                            f.travelTimeMins
                    )
            );
        }


        // Call
        view.findViewById(
                R.id.btnCallFacility
        ).setOnClickListener(v -> {

            startActivity(
                    new Intent(
                            Intent.ACTION_DIAL,
                            Uri.parse(
                                    "tel:" + f.phone
                            )
                    )
            );
        });


        // Navigate / zoom map
        view.findViewById(
                R.id.btnNavFacility
        ).setOnClickListener(v -> {

            if (isMapLoaded) {

                mapWebView.evaluateJavascript(
                        "zoomTo("
                                + f.lat
                                + ","
                                + f.lng
                                + ");",
                        null
                );
            }
        });


        container.addView(view);
    }


    // =========================================================
    // JAVASCRIPT ESCAPE
    // =========================================================

    private String escapeJavaScript(String text) {

        if (text == null) {
            return "";
        }

        return text
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\"", "\\\"");
    }
}