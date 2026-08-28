package com.example.emergency_sos_app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class ResetPasswordActivity extends BaseActivity {

    private EditText etNewPassword, etConfirmPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        Button btnReset = findViewById(R.id.btnReset);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnReset.setOnClickListener(v -> resetPassword());
    }

    private void resetPassword() {
        String newPass = etNewPassword.getText().toString().trim();
        String confirmPass = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(newPass)) {
            etNewPassword.setError("Required");
            return;
        }

        if (newPass.length() < 6) {
            etNewPassword.setError("Minimum 6 characters");
            return;
        }

        if (!newPass.equals(confirmPass)) {
            etConfirmPassword.setError(getString(R.string.password_mismatch));
            return;
        }

        // Update password in prefs
        SharedPreferences sp = getSharedPreferences("sos_profile_prefs", MODE_PRIVATE);
        sp.edit().putString("password", newPass).apply();

        Toast.makeText(this, getString(R.string.password_reset_success), Toast.LENGTH_LONG).show();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
