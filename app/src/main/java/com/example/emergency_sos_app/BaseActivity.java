package com.example.emergency_sos_app;

import android.content.Intent;
import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Base activity to provide shared logic like language support, edge-to-edge UI, and smooth transitions.
 */
public class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // Enable Edge-to-Edge before super.onCreate
        EdgeToEdge.enable(this);
        
        // Apply language before layout inflation
        LanguageManager.apply(this);
        super.onCreate(savedInstanceState);

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
}
