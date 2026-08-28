package com.example.emergency_sos_app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SettingsActivity extends BaseActivity {

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences("sos_profile_prefs", Context.MODE_PRIVATE);

        setupEdgeToEdge();
        initViews();
    }

    private void initViews() {
        TextView tvUserName = findViewById(R.id.tvUserName);
        TextView tvUserEmail = findViewById(R.id.tvUserEmail);
        TextView tvLang = findViewById(R.id.tvCurrentLanguage);
        Switch switchNotifications = findViewById(R.id.switchNotifications);
        Switch switchSimulation = findViewById(R.id.switchSimulation);
        Button btnLogout = findViewById(R.id.btnLogout);

        tvUserName.setText(prefs.getString("name", "Guest"));
        tvUserEmail.setText(prefs.getString("email", "not signed in"));
        
        String lang = LanguageManager.getLanguage(this);
        tvLang.setText(LanguageManager.LANG_KHMER.equals(lang) ? "ភាសាខ្មែរ" : "English");

        switchNotifications.setChecked(prefs.getBoolean("notifications_enabled", true));
        switchSimulation.setChecked(prefs.getBoolean("simulation_mode", false));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        findViewById(R.id.rowLanguage).setOnClickListener(v -> LanguagePickerDialog.show(this));

        switchNotifications.setOnCheckedChangeListener((v, checked) -> 
                prefs.edit().putBoolean("notifications_enabled", checked).apply());

        switchSimulation.setOnCheckedChangeListener((v, checked) -> 
                prefs.edit().putBoolean("simulation_mode", checked).apply());

        findViewById(R.id.rowPrivacy).setOnClickListener(v -> navigateToDetail("privacy"));
        findViewById(R.id.rowHelp).setOnClickListener(v -> navigateToDetail("help"));

        findViewById(R.id.rowClearHistory).setOnClickListener(v -> {
            HistoryManager.clear(this);
            Toast.makeText(this, "Emergency history cleared", Toast.LENGTH_SHORT).show();
        });

        btnLogout.setOnClickListener(v -> {
            prefs.edit().putBoolean("remember", false).apply();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void setupEdgeToEdge() {
        View header = findViewById(R.id.settingsHeader);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            if (header != null) header.setPadding(header.getPaddingLeft(), top + (int)(16 * getResources().getDisplayMetrics().density), header.getPaddingRight(), (int)(16 * getResources().getDisplayMetrics().density));
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void navigateToDetail(String type) {
        Intent intent = new Intent(this, SettingsDetailActivity.class);
        intent.putExtra("TYPE", type);
        startActivity(intent);
    }
}
