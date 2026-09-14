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

/**
 * Professional Splash Screen that handles initial routing and pre-loading.
 * Features a dynamic logo spin and grow animation sequence.
 */
public class SplashActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        View logo = findViewById(R.id.ivSplashLogo);
        View tagline = findViewById(R.id.layoutSplashTagline);

        // Pre-initialize background systems
        TranslationManager.getInstance().initialize(this);
        SocketManager.getInstance().connect(null);

        // --- ANIMATION SEQUENCE ---

        // 1. Initial State: Small and Invisible
        logo.setScaleX(0.1f);
        logo.setScaleY(0.1f);
        logo.setAlpha(0f);

        // 2. Rotation Animation (Spin round) - Professional faster entry
        logo.animate()
                .rotation(720f)
                .setDuration(800)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        // 3. Grow and Fade-In Animation - Rapid pop-in
        logo.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .alpha(1.0f)
                .setDuration(800)
                .setStartDelay(100) // Minimal delay
                .setInterpolator(new OvershootInterpolator(1.2f))
                .withEndAction(() -> {
                    // 4. Reveal Tagline
                    if (tagline != null) {
                        tagline.setVisibility(View.VISIBLE);
                        tagline.setAlpha(0f);
                        tagline.animate().alpha(1.0f).setDuration(300).start();
                    }
                    
                    // Reduced delay for instant transition
                    new Handler(Looper.getMainLooper()).postDelayed(this::performRouting, 400);
                })
                .start();
    }

    private void performRouting() {
        SharedPreferences sp = getSharedPreferences("sos_profile_prefs", MODE_PRIVATE);
        
        // --- EMERGENCY PERSISTENCE CHECK ---
        boolean isSosActive = sp.getBoolean("is_sos_active", false);
        if (isSosActive) {
            String activeId = sp.getString("active_sos_id", null);
            String activeType = sp.getString("active_sos_type", "MEDICAL");
            
            Log.d("Splash", "Emergency mode persist: " + activeId);
            Intent intent = new Intent(this, SosWorkflowActivity.class);
            intent.putExtra("SOS_ID", activeId);
            intent.putExtra("INCIDENT_TYPE", activeType);
            startFadeActivity(intent);
            finish();
            return;
        }

        String email = sp.getString("email", "");
        boolean remember = sp.getBoolean("remember", false);

        Intent intent;
        if (email.isEmpty()) {
            // New User -> Welcome Screen
            intent = new Intent(this, WelcomeActivity.class);
        } else if (!remember) {
            // Logged out -> Login Screen
            intent = new Intent(this, LoginActivity.class);
        } else {
            // Authenticated
            boolean safetyDone = sp.getBoolean("safety_check_done", false);
            if (!safetyDone) {
                intent = new Intent(this, SafetyCheckActivity.class);
            } else {
                intent = new Intent(this, DashboardActivity.class);
                intent.putExtra("USER_NAME", sp.getString("name", "User"));
            }
        }

        startFadeActivity(intent);
        finish();
    }
}
