package com.example.emergency_sos_app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

/**
 * Professional Splash Screen that handles initial routing and pre-loading.
 * Features an intelligent routing system based on user account status.
 */
public class SplashActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        View logo = findViewById(R.id.ivSplashLogo);
        View tagline = findViewById(R.id.layoutSplashTagline);
        TextView tvStatus = findViewById(R.id.tvSplashStatus);

        // Pre-initialize background systems
        SocketManager.getInstance().connect(null);

        // --- ANIMATION SEQUENCE ---

        // 1. Initial State
        logo.setScaleX(0.1f);
        logo.setScaleY(0.1f);
        logo.setAlpha(0f);

        // 2. Main Logo Animation
        logo.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .alpha(1.0f)
                .setDuration(2000)
                .setInterpolator(new OvershootInterpolator(1.2f))
                .withEndAction(() -> {
                    // 3. Reveal Tagline and Status
                    if (tagline != null) {
                        tagline.setVisibility(View.VISIBLE);
                        tagline.setAlpha(0f);
                        tagline.animate().alpha(1.0f).setDuration(500).start();
                    }
                    
                    if (tvStatus != null) {
                        tvStatus.animate().alpha(1.0f).setDuration(800).start();
                    }
                    
                    // Route after system "initialization"
                    new Handler(Looper.getMainLooper()).postDelayed(this::performRouting, 1500);
                })
                .start();
    }

    private void performRouting() {
        SharedPreferences sp = getSharedPreferences("sos_profile_prefs", MODE_PRIVATE);
        
        // --- 1. EMERGENCY PERSISTENCE ---
        boolean isSosActive = sp.getBoolean("is_sos_active", false);
        if (isSosActive) {
            Intent intent = new Intent(this, SosWorkflowActivity.class);
            intent.putExtra("SOS_ID", sp.getString("active_sos_id", null));
            intent.putExtra("INCIDENT_TYPE", sp.getString("active_sos_type", "MEDICAL"));
            intent.putExtra("IS_SILENT", sp.getBoolean("active_sos_silent", false));
            startFadeActivity(intent);
            finish();
            return;
        }

        // --- 2. INTELLIGENT ROUTING ---
        boolean hasAccount = sp.getBoolean("has_account", false);
        boolean remember = sp.getBoolean("remember", false);
        String email = sp.getString("email", "");

        Intent intent;
        if (!hasAccount || email.isEmpty()) {
            // BRAND NEW USER -> Welcome Screen (Tutorial)
            intent = new Intent(this, WelcomeActivity.class);
        } else if (!remember) {
            // HAS ACCOUNT BUT LOGGED OUT -> Direct to Login
            intent = new Intent(this, LoginActivity.class);
        } else {
            // AUTHENTICATED -> Dashboard
            intent = new Intent(this, DashboardActivity.class);
            intent.putExtra("USER_NAME", sp.getString("name", "User"));
        }

        startFadeActivity(intent);
        finish();
    }
}
