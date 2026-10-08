package com.example.emergency_sos_app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

public class SettingsDetailActivity extends BaseActivity {

    @Override
    protected void onResume() {
        super.onResume();
        View privacyPanel = findViewById(R.id.privacyPanel);
        if (privacyPanel != null && privacyPanel.getVisibility() == View.VISIBLE) {
            populatePermissionStatuses();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_detail);

        ImageView btnBack = findViewById(R.id.btnBack);
        TextView tvTitle = findViewById(R.id.tvTitle);
        TextView tvContent = findViewById(R.id.tvContent);
        View privacyPanel = findViewById(R.id.privacyPanel);

        String type = getIntent().getStringExtra("TYPE");
        if ("privacy".equals(type)) {
            tvTitle.setText(R.string.privacy_security);
            tvContent.setText(R.string.privacy_intro);
            privacyPanel.setVisibility(View.VISIBLE);
            populatePermissionStatuses();
            findViewById(R.id.btnManagePermissions).setOnClickListener(v -> openAppSettings());
        } else if ("help".equals(type)) {
            tvTitle.setText(R.string.help_support);
            tvContent.setText("Welcome to the Emergency SOS Support Center. Here is how to maximize your safety:\n\n" +
                    "1. In a real emergency, hold the SOS button until the confirmation flow starts.\n\n" +
                    "2. Family Circle: Keep emergency contacts up to date. Contact sharing depends on the feature used and available network services.\n\n" +
                    "3. Dashboard Map: The background map shows your real-time position. If you see 'Location Locked', please check your GPS settings.\n\n" +
                    "4. Contacting Authorities: Use the Action Hub to directly dial 117 (Police) or 118 (Fire/Ambulance) without leaving the app.\n\n" +
                    "5. Connectivity: SOS requests may not reach online services without a data connection. If needed, call local emergency services directly.");
            privacyPanel.setVisibility(View.GONE);
        } else {
            finish();
            return;
        }

        btnBack.setOnClickListener(v -> finish());
    }

    private void populatePermissionStatuses() {
        setPermissionStatus(R.id.tvLocationPermission, R.string.permission_location,
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED);
        setPermissionStatus(R.id.tvCameraPermission, R.string.permission_camera,
                ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED);
        setPermissionStatus(R.id.tvMicrophonePermission, R.string.permission_microphone,
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED);

        boolean notificationsGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        setPermissionStatus(R.id.tvNotificationPermission, R.string.permission_notifications, notificationsGranted);
    }

    private void setPermissionStatus(int viewId, int permissionNameId, boolean granted) {
        TextView view = findViewById(viewId);
        view.setText(getString(R.string.permission_status_format, getString(permissionNameId),
                getString(granted ? R.string.permission_allowed : R.string.permission_not_allowed)));
        view.setTextColor(ContextCompat.getColor(this, granted ? R.color.text_dark : R.color.sos_red));
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", getPackageName(), null));
        startActivity(intent);
    }
}