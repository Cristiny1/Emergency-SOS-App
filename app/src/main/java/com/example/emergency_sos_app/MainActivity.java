package com.example.emergency_sos_app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;


public class MainActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LanguageManager.apply(this);
        setContentView(R.layout.activity_welcome);

        Button btnGetStarted = findViewById(R.id.btnGetStarted);
        TextView tvGoToSignIn = findViewById(R.id.tvGoToSignIn);
        Button btnSwitchLanguage = findViewById(R.id.btnSwitchLanguage);

        // "Get Started" -> Sign Up screen (new user)
        btnGetStarted.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SignupActivity.class);
            startActivity(intent);
        });

        // "Already have an account? Sign In" -> Login screen (existing user)
        tvGoToSignIn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        btnSwitchLanguage.setOnClickListener(v -> toggleLanguage());
    }

    private void toggleLanguage() {
        String current = LanguageManager.getLanguage(this);
        String nextLanguage = LanguageManager.LANG_KHMER.equals(current) ? LanguageManager.LANG_ENGLISH : LanguageManager.LANG_KHMER;
        LanguageManager.setLanguage(this, nextLanguage);
        recreate();
    }
}