package com.example.emergency_sos_app;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import androidx.core.app.ActivityCompat;

public class SafetyCheckActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_safety_check);

        CheckBox checkLocation = findViewById(R.id.checkLocation);
        CheckBox checkNotification = findViewById(R.id.checkNotification);
        Button btnContinue = findViewById(R.id.btnContinue);

        // Update visual checkmarks based on real permissions
        boolean hasLocation = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        checkLocation.setChecked(hasLocation);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            boolean hasNotify = ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
            checkNotification.setChecked(hasNotify);
        }

        btnContinue.setOnClickListener(v -> {
            SharedPreferences sp = getSharedPreferences("sos_profile_prefs", MODE_PRIVATE);
            sp.edit().putBoolean("safety_check_done", true).apply();
            
            startFadeActivity(new Intent(this, DashboardActivity.class));
            finish();
        });
    }
}
