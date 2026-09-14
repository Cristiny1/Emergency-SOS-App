package com.example.emergency_sos_app;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;


public class SignupActivity extends BaseActivity {

    private EditText etFullName, etEmail, etPassword, etConfirm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirm = findViewById(R.id.etConfirm);

        Button btnCreateAccount = findViewById(R.id.btnCreateAccount);
        TextView tvGoToSignIn = findViewById(R.id.tvGoToSignIn);

        FrameLayout btnGoogleCircle = findViewById(R.id.btnGoogleCircle);
        FrameLayout btnFacebookCircle = findViewById(R.id.btnFacebookCircle);

        btnCreateAccount.setOnClickListener(v -> attemptSignup());

        tvGoToSignIn.setOnClickListener(v -> {
            Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

        btnGoogleCircle.setOnClickListener(v ->
                Toast.makeText(this, "Google Sign Up coming soon.", Toast.LENGTH_SHORT).show());

        btnFacebookCircle.setOnClickListener(v ->
                Toast.makeText(this, "Facebook Sign Up coming soon.", Toast.LENGTH_SHORT).show());
    }

    private void attemptSignup() {

        String name = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirm = etConfirm.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            etFullName.setError(getString(R.string.name_required));
            etFullName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            etEmail.setError(getString(R.string.email_required));
            etEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError(getString(R.string.invalid_email));
            etEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError(getString(R.string.enter_password));
            etPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            etPassword.setError(getString(R.string.password_min_length));
            etPassword.requestFocus();
            return;
        }

        if (!password.equals(confirm)) {
            etConfirm.setError(getString(R.string.password_mismatch));
            etConfirm.requestFocus();
            return;
        }

        // Determine Delivery Method - Default to EMAIL since phone is removed from signup
        OtpManager.DeliveryMethod method = OtpManager.DeliveryMethod.EMAIL;
        String destination = email;

        // Trigger OTP Verification
        OtpManager.getInstance().sendOtp(this, destination, method);

        Intent intent = new Intent(SignupActivity.this, OtpVerificationActivity.class);
        intent.putExtra("NAME", name);
        intent.putExtra("EMAIL", email);
        intent.putExtra("PASSWORD", password);
        intent.putExtra("DESTINATION", destination);
        intent.putExtra("METHOD", method.name());
        startFadeActivity(intent);
    }
}
