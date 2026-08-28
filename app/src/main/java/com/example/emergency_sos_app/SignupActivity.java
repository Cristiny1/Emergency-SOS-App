package com.example.emergency_sos_app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;


public class SignupActivity extends BaseActivity {

    private EditText etFullName, etEmail, etPassword, etPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);

        Button btnCreateAccount = findViewById(R.id.btnCreateAccount);
        TextView tvGoToSignIn = findViewById(R.id.tvGoToSignIn);

        FrameLayout btnGoogleCircle = findViewById(R.id.btnGoogleCircle);
        // FrameLayout btnAppleCircle = findViewById(R.id.btnAppleCircle);
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
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            etFullName.setError("Full Name is required");
            etFullName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Invalid Email");
            etEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(phone)) {
            etPhone.setError("Phone Number is required");
            etPhone.requestFocus();
            return;
        }

        if (phone.length() < 8) {
            etPhone.setError("Invalid Phone Number");
            etPhone.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return;
        }

        // Determine Delivery Method
        OtpManager.DeliveryMethod method = OtpManager.DeliveryMethod.SMS;
        String destination = phone;
        if (((android.widget.RadioGroup) findViewById(R.id.rgVerifyMethod)).getCheckedRadioButtonId() == R.id.rbEmail) {
            method = OtpManager.DeliveryMethod.EMAIL;
            destination = email;
        }

        // Trigger OTP Verification
        OtpManager.getInstance().sendOtp(this, destination, method);

        Intent intent = new Intent(SignupActivity.this, OtpVerificationActivity.class);
        intent.putExtra("NAME", name);
        intent.putExtra("EMAIL", email);
        intent.putExtra("PHONE", phone);
        intent.putExtra("PASSWORD", password);
        intent.putExtra("DESTINATION", destination);
        intent.putExtra("METHOD", method.name());
        startActivity(intent);
    }
}