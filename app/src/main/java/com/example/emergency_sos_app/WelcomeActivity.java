package com.example.emergency_sos_app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Entry point activity for new or unauthenticated users.
 * Tutorial and initial navigation options.
 */
public class WelcomeActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        // Flag Switcher Logic
        ImageView flagButton = findViewById(R.id.btn_language_flag);
        if (flagButton != null) {
            flagButton.setImageResource(LanguageManager.getFlagDrawable(this));
            flagButton.setOnClickListener(v -> LanguagePickerDialog.show(this));
        }

        Button btnGetStarted = findViewById(R.id.btnGetStarted);
        TextView tvGoToSignIn = findViewById(R.id.tvGoToSignIn);

        // Transitions using BaseActivity helper
        btnGetStarted.setOnClickListener(v -> navigate(true));
        tvGoToSignIn.setOnClickListener(v -> navigate(false));
    }

    private void navigate(boolean forSignUp) {
        Intent intent = forSignUp ? new Intent(this, SignupActivity.class) : new Intent(this, LoginActivity.class);
        startFadeActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ensure flag is correct after language change recreation
        ImageView flagButton = findViewById(R.id.btn_language_flag);
        if (flagButton != null) {
            flagButton.setImageResource(LanguageManager.getFlagDrawable(this));
        }
    }
}
