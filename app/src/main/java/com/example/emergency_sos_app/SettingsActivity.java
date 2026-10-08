package com.example.emergency_sos_app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.app.NotificationManager;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.io.File;

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
        ImageView ivProfile = findViewById(R.id.ivSettingsProfile);
        SwitchMaterial switchNotifications = findViewById(R.id.switchNotifications);
        SwitchMaterial switchSimulation = findViewById(R.id.switchSimulation);
        Button btnLogout = findViewById(R.id.btnLogout);

        // Populate User Info
        tvUserName.setText(prefs.getString("name", "Guest"));
        tvUserEmail.setText(prefs.getString("email", "not signed in"));

        if (ivProfile != null) {
            String photoPath = prefs.getString("profile_photo_path", null);
            if (photoPath != null) {
                File file = new File(photoPath);
                if (file.exists()) ivProfile.setImageURI(Uri.fromFile(file));
            }
        }
        
        // Language Display
        String lang = LanguageManager.getLanguage(this);
        tvLang.setText(LanguageManager.LANG_KHMER.equals(lang) ? "ភាសាខ្មែរ" : "English");

        // Toggle States
        switchNotifications.setChecked(prefs.getBoolean("notifications_enabled", true));
        switchSimulation.setChecked(prefs.getBoolean("simulation_mode", false));

        // Listeners
        findViewById(R.id.btnBack).setOnClickListener(v -> {
            playClickFeedback();
            finish();
        });
        
        findViewById(R.id.cardAccount).setOnClickListener(v -> {
            playClickFeedback();
            startFadeActivity(new Intent(this, AccountDetailActivity.class));
        });

        findViewById(R.id.rowLanguage).setOnClickListener(v -> {
            playClickFeedback();
            LanguagePickerDialog.show(this);
        });

        findViewById(R.id.rowNotifications).setOnClickListener(v -> {
            switchNotifications.setChecked(!switchNotifications.isChecked());
        });

        switchNotifications.setOnCheckedChangeListener((v, checked) -> {
            playClickFeedback();
            prefs.edit().putBoolean("notifications_enabled", checked).apply();
            if (!checked) {
                NotificationManager notificationManager = getSystemService(NotificationManager.class);
                if (notificationManager != null) notificationManager.cancel(101);
            }
        });

        findViewById(R.id.rowSimulation).setOnClickListener(v -> {
            switchSimulation.setChecked(!switchSimulation.isChecked());
        });

        switchSimulation.setOnCheckedChangeListener((v, checked) -> {
            playClickFeedback();
            prefs.edit().putBoolean("simulation_mode", checked).apply();
            String msg = checked ? getString(R.string.simulation_mode_enabled) : getString(R.string.realtime_mode_active);
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.rowPrivacy).setOnClickListener(v -> {
            playClickFeedback();
            navigateToDetail("privacy");
        });

        findViewById(R.id.rowHelp).setOnClickListener(v -> {
            playClickFeedback();
            navigateToDetail("help");
        });

        findViewById(R.id.rowClearHistory).setOnClickListener(v -> {
            playClickFeedback();
            new AlertDialog.Builder(this)
                    .setTitle(R.string.clear_history_title)
                    .setMessage(R.string.clear_history_message)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.clear_history, (dialog, which) -> {
                        HistoryManager.clear(this);
                        Toast.makeText(this, R.string.history_cleared, Toast.LENGTH_SHORT).show();
                    })
                    .show();
        });

        btnLogout.setOnClickListener(v -> {
            playClickFeedback();
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
            if (header != null) {
                header.setPadding(header.getPaddingLeft(), top, header.getPaddingRight(), header.getPaddingBottom());
            }
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void navigateToDetail(String type) {
        Intent intent = new Intent(this, SettingsDetailActivity.class);
        intent.putExtra("TYPE", type);
        startActivity(intent);
    }
}
