package com.example.emergency_sos_app;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Base activity to provide shared logic like language support, edge-to-edge UI, and smooth transitions.
 */
public class BaseActivity extends AppCompatActivity {

    protected Vibrator vibrator;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // Enable Edge-to-Edge before super.onCreate
        EdgeToEdge.enable(this);
        
        // Apply language before layout inflation
        LanguageManager.apply(this);
        super.onCreate(savedInstanceState);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // Ensure any creation/recreation (like language change) is smooth
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    public void finish() {
        super.finish();
        // Standard smooth transition when closing an activity
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    /** Helper to start activities with a uniform transition */
    protected void startFadeActivity(Intent intent) {
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    /** Professional activity refresh without the standard "recreate" flash */
    public void smoothRefresh() {
        Intent intent = getIntent();
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    /**
     * Standardized Bottom Navigation setup for all inheriting activities.
     * Fixes height inconsistencies and edge-to-edge margin bugs.
     */
    protected void setupNavigation(int currentId) {
        NavigationHelper.setup(this, currentId);
        
        View bottomNav = findViewById(R.id.bottomNavigation);
        if (bottomNav != null) {
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
                int navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
                
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) bottomNav.getLayoutParams();
                float density = getResources().getDisplayMetrics().density;
                lp.bottomMargin = (int) (16 * density) + navBarHeight;
                bottomNav.setLayoutParams(lp);
                
                return WindowInsetsCompat.CONSUMED;
            });
        }
    }

    /** App-wide haptic feedback utility */
    public void playClickFeedback() {
        View root = findViewById(android.R.id.content);
        if (root != null) {
            root.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        }
    }

    /** Unified vibration helper */
    protected void vibrate(long millis) {
        if (vibrator == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(millis);
        }
    }

    protected boolean isNetworkAvailable() {
        android.net.ConnectivityManager cm = (android.net.ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        android.net.NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }
}
