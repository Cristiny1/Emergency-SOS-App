package com.example.emergency_sos_app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.List;
import java.util.Locale;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class CitizenReportActivity extends BaseActivity {

    private ImageView ivPreview;
    private LinearLayout layoutUploadHint;
    private TextView tvReportAddress, tvCriticalWarning;
    private TextView tvSubmittingState;
    private ProgressBar progressSubmitting;
    private WebView mapWebView;
    private ChipGroup chipGroupSeverity;
    private SwitchMaterial switchAnonymous;
    
    private boolean isMapLoaded = false;
    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<String> photoPickerLauncher;

    private double currentLat = 13.3633;
    private double currentLng = 103.8564;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_citizen_report);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        
        ivPreview = findViewById(R.id.ivPreview);
        layoutUploadHint = findViewById(R.id.layoutUploadHint);
        tvReportAddress = findViewById(R.id.tvReportAddress);
        tvCriticalWarning = findViewById(R.id.tvCriticalWarning);
        tvSubmittingState = findViewById(R.id.tvSubmittingState);
        progressSubmitting = findViewById(R.id.progressSubmitting);
        mapWebView = findViewById(R.id.mapWebView);
        chipGroupSeverity = findViewById(R.id.chipGroupSeverity);
        switchAnonymous = findViewById(R.id.switchAnonymous);
        
        Button btnSubmit = findViewById(R.id.btnSubmitReport);

        setupWebView();
        setupSeverityLogic();

        photoPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        ivPreview.setImageURI(uri);
                        layoutUploadHint.setVisibility(View.GONE);
                    }
                });

        findViewById(R.id.btnUploadPhoto).setOnClickListener(v -> photoPickerLauncher.launch("image/*"));
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnVoiceRecord).setOnClickListener(v -> 
                Toast.makeText(this, "Voice recording feature coming soon.", Toast.LENGTH_SHORT).show());

        btnSubmit.setOnClickListener(v -> {
            EditText etDesc = findViewById(R.id.etDescription);
            if (etDesc.getText().toString().trim().isEmpty()) {
                etDesc.setError(getString(R.string.report_description_required));
                return;
            }

            setSubmittingState(true);

            // Collect Data
            Spinner spinnerCategory = findViewById(R.id.spinnerIssueCategory);
            String category = spinnerCategory.getSelectedItem().toString();
            String severity = getSelectedSeverity();
            boolean isAnonymous = switchAnonymous.isChecked();

            SharedPreferences prefs = getSharedPreferences("sos_profile_prefs", MODE_PRIVATE);
            prefs.edit()
                    .putBoolean("report_in_progress", true)
                    .putString("active_report_category", category)
                    .apply();

            // REAL-TIME BACKEND EMISSION
            try {
                org.json.JSONObject payload = new org.json.JSONObject();
                payload.put("reportId", "RPT_" + System.currentTimeMillis());
                payload.put("category", category);
                payload.put("severity", severity);
                payload.put("isAnonymous", isAnonymous);
                payload.put("description", etDesc.getText().toString().trim());
                payload.put("latitude", currentLat);
                payload.put("longitude", currentLng);
                payload.put("timestamp", System.currentTimeMillis());
                payload.put("status", "SUBMITTED");
                
                SocketManager.getInstance().emit("citizen_report", payload);
                android.util.Log.d("REPORT", "Report Emitted: " + payload.toString());
            } catch (Exception e) {
                android.util.Log.e("REPORT", "Failed to emit report", e);
            }

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Toast.makeText(this, R.string.report_submitted_center, Toast.LENGTH_LONG).show();
                finish();
            }, 1500);
        });

        requestLocation();
    }

    private void setupSeverityLogic() {
        chipGroupSeverity.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipCritical) {
                tvCriticalWarning.setVisibility(View.VISIBLE);
            } else {
                tvCriticalWarning.setVisibility(View.GONE);
            }
        });
    }

    private String getSelectedSeverity() {
        int id = chipGroupSeverity.getCheckedChipId();
        if (id == R.id.chipCritical) return "CRITICAL";
        if (id == R.id.chipHigh) return "HIGH";
        if (id == R.id.chipMedium) return "MEDIUM";
        return "LOW";
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings settings = mapWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        mapWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                isMapLoaded = true;
                updateWebViewLocation();
            }
        });

        mapWebView.loadUrl("file:///android_asset/leaflet_map.html");
    }

    private void requestLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    currentLat = location.getLatitude();
                    currentLng = location.getLongitude();
                    updateWebViewLocation();
                    updateAddressLabel(currentLat, currentLng);
                } else {
                    updateAddressLabel(currentLat, currentLng);
                }
            });
        } else {
            updateAddressLabel(currentLat, currentLng);
        }
    }

    private void updateWebViewLocation() {
        if (!isMapLoaded) return;
        mapWebView.evaluateJavascript("setLocation(" + currentLat + "," + currentLng + ", 'Report Location')", null);
    }

    private void setSubmittingState(boolean submitting) {
        Button btnSubmit = findViewById(R.id.btnSubmitReport);
        if (btnSubmit != null) {
            btnSubmit.setEnabled(!submitting);
            btnSubmit.setText(submitting ? getString(R.string.report_submitting) : getString(R.string.report_submit_button));
        }
        if (progressSubmitting != null) {
            progressSubmitting.setVisibility(submitting ? View.VISIBLE : View.GONE);
        }
        if (tvSubmittingState != null) {
            tvSubmittingState.setVisibility(submitting ? View.VISIBLE : View.GONE);
            tvSubmittingState.setText(submitting ? getString(R.string.report_processing_message) : "");
        }
    }

    private void updateAddressLabel(double lat, double lng) {
        new Thread(() -> {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            String resolvedAddress = null;
            try {
                List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address addr = addresses.get(0);
                    resolvedAddress = addr.getAddressLine(0);
                    if (resolvedAddress != null && !resolvedAddress.toLowerCase().contains("cambodia")) {
                        resolvedAddress += ", Cambodia";
                    }
                }
            } catch (Exception e) {
                resolvedAddress = String.format(Locale.getDefault(), "Lat: %.4f, Lng: %.4f", lat, lng);
            }

            final String finalAddr = resolvedAddress;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed() || tvReportAddress == null) return;
                tvReportAddress.setText(finalAddr != null ? finalAddr : getString(R.string.report_detecting_location));
            });
        }).start();
    }
}
