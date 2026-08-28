package com.example.emergency_sos_app;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SosCenterActivity extends BaseActivity {

    private TextView tvCenterLogs;
    private StringBuilder logs = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sos_center);

        tvCenterLogs = findViewById(R.id.tvCenterLogs);
        TextView tvUser = findViewById(R.id.tvCenterUser);
        TextView tvType = findViewById(R.id.tvCenterType);
        TextView tvLoc = findViewById(R.id.tvCenterLocation);
        
        Button btnAssign = findViewById(R.id.btnCenterAssign);
        Button btnResolve = findViewById(R.id.btnCenterResolve);
        Button btnClose = findViewById(R.id.btnCenterClose);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Fetch User Data for Simulation
        SharedPreferences prefs = getSharedPreferences("sos_profile_prefs", MODE_PRIVATE);
        String name = prefs.getString("name", "Demo User");
        String category = prefs.getString("active_report_category", "Medical Emergency");
        
        tvUser.setText("User: " + name);
        tvType.setText("Type: " + category);
        tvLoc.setText(R.string.detecting_location);

        addLog("SOS Center Console Initialized.");
        addLog("Connected to dispatch server.");
        addLog("Active Alert: " + category + " from " + name);

        btnAssign.setOnClickListener(v -> {
            updateGlobalStatus("ASSIGNED");
            addLog("Manual override: Rescue team assigned.");
            Toast.makeText(this, "Status updated to ASSIGNED", Toast.LENGTH_SHORT).show();
        });

        btnResolve.setOnClickListener(v -> {
            updateGlobalStatus("COMPLETED");
            addLog("Case resolved and closed by operator.");
            Toast.makeText(this, "Status updated to COMPLETED", Toast.LENGTH_SHORT).show();
        });

        btnClose.setOnClickListener(v -> finish());
    }

    private void addLog(String message) {
        String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        logs.append("[").append(time).append("] ").append(message).append("\n");
        tvCenterLogs.setText(logs.toString());
    }

    private void updateGlobalStatus(String status) {
        SharedPreferences prefs = getSharedPreferences("sos_profile_prefs", MODE_PRIVATE);
        prefs.edit().putString("simulation_status_override", status).apply();
    }
}