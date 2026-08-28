package com.example.emergency_sos_app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

        // 2. Rotation Animation (Spin round)
        logo.animate()
                .rotation(720f)
                .setDuration(2200)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        // 3. Grow and Fade-In Animation
        logo.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .alpha(1.0f)
                .setDuration(2000)
                .setStartDelay(400)
                .setInterpolator(new OvershootInterpolator(1.2f))
                .withEndAction(() -> {
                    // 4. Reveal Tagline
                    if (tagline != null) {
                        tagline.setVisibility(View.VISIBLE);
                        tagline.setAlpha(0f);
                        tagline.animate().alpha(1.0f).setDuration(600).start();
                    }
                    
                    // Final delay before routing to allow user to see branding
                    new Handler(Looper.getMainLooper()).postDelayed(this::performRouting, 1200);
                })
                .start();
    }

    private void performRouting() {
        SharedPreferences sp = getSharedPreferences("sos_profile_prefs", MODE_PRIVATE);
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
            // Authenticated -> Dashboard
            intent = new Intent(this, DashboardActivity.class);
            intent.putExtra("USER_NAME", sp.getString("name", "User"));
        }

        startFadeActivity(intent);
        finish();
    }
}
