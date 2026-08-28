package com.example.emergency_sos_app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;


public class LoginActivity extends BaseActivity {

    private EditText etEmail, etPassword;
    private CheckBox cbRememberMe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        cbRememberMe = findViewById(R.id.cbRememberMe);

        Button btnSignIn = findViewById(R.id.btnSignIn);
        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);
        TextView tvGoToSignUp = findViewById(R.id.tvGoToSignUp);

        FrameLayout btnGoogleCircle = findViewById(R.id.btnGoogleCircle);
        // FrameLayout btnAppleCircle = findViewById(R.id.btnAppleCircle);
        FrameLayout btnFacebookCircle = findViewById(R.id.btnFacebookCircle);

        SharedPreferences sp = getSharedPreferences("sos_profile_prefs", MODE_PRIVATE);

        // Auto Login
        if (sp.getBoolean("remember", false)) {
            Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
            intent.putExtra("USER_NAME", sp.getString("name", "User"));
            startActivity(intent);
            finish();
        }

        btnSignIn.setOnClickListener(v -> attemptLogin());

        tvForgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
        });

        tvGoToSignUp.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignupActivity.class));
        });

        btnGoogleCircle.setOnClickListener(v ->
                Toast.makeText(this,
                        "Google Sign In coming soon.",
                        Toast.LENGTH_SHORT).show());

        btnFacebookCircle.setOnClickListener(v ->
                Toast.makeText(this,
                        "Facebook Sign In coming soon.",
                        Toast.LENGTH_SHORT).show());
    }

    private void attemptLogin() {

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }

        SharedPreferences sp = getSharedPreferences("sos_profile_prefs", MODE_PRIVATE);

        String savedEmail = sp.getString("email", "admin@gmail.com");
        String savedPassword = sp.getString("password", "123456");
        String savedName = sp.getString("name", "Admin");

        if (email.equals(savedEmail) && password.equals(savedPassword)) {

            SharedPreferences.Editor editor = sp.edit();

            if (cbRememberMe.isChecked()) {
                editor.putBoolean("remember", true);
            } else {
                editor.putBoolean("remember", false);
            }

            editor.apply();

            Toast.makeText(this,
                    "Login Successful",
                    Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
            intent.putExtra("USER_NAME", savedName);
            startActivity(intent);
            finish();

        } else {

            Toast.makeText(this,
                    "Invalid Email or Password",
                    Toast.LENGTH_SHORT).show();

        }
    }
}