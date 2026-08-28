package com.example.emergency_sos_app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

public class ForgotPasswordActivity extends BaseActivity {

    private EditText etIdentifier;
    private TextView tvLabel;
    private RadioGroup rgMethod;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        etIdentifier = findViewById(R.id.etIdentifier);
        tvLabel = findViewById(R.id.tvIdentifierLabel);
        rgMethod = findViewById(R.id.rgRecoveryMethod);
        Button btnSendCode = findViewById(R.id.btnSendCode);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        rgMethod.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbPhone) {
                tvLabel.setText(R.string.phone_label);
                etIdentifier.setHint(R.string.enter_phone);
                etIdentifier.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
            } else {
                tvLabel.setText(R.string.email_label);
                etIdentifier.setHint(R.string.enter_email);
                etIdentifier.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS | android.text.InputType.TYPE_CLASS_TEXT);
            }
            etIdentifier.setText("");
            etIdentifier.setError(null);
            etIdentifier.requestFocus();
        });

        btnSendCode.setOnClickListener(v -> attemptSendCode());
    }

    private void attemptSendCode() {
        String identifier = etIdentifier.getText().toString().trim();
        boolean isPhone = rgMethod.getCheckedRadioButtonId() == R.id.rbPhone;

        if (TextUtils.isEmpty(identifier)) {
            etIdentifier.setError("Required");
            etIdentifier.requestFocus();
            return;
        }

        // Check if identifier is registered
        SharedPreferences sp = getSharedPreferences("sos_profile_prefs", MODE_PRIVATE);
        String registeredValue = isPhone ? sp.getString("phone", "") : sp.getString("email", "");

        if (!identifier.equalsIgnoreCase(registeredValue)) {
            String errorMsg = isPhone ? getString(R.string.phone_not_registered) : "This email is not registered";
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
            return;
        }

        // Send OTP
        OtpManager.DeliveryMethod method = isPhone ? OtpManager.DeliveryMethod.SMS : OtpManager.DeliveryMethod.EMAIL;
        OtpManager.getInstance().sendOtp(this, identifier, method);

        Intent intent = new Intent(this, OtpVerificationActivity.class);
        intent.putExtra("DESTINATION", identifier);
        intent.putExtra("METHOD", method.name());
        intent.putExtra("IS_RESET_FLOW", true);
        startActivity(intent);
    }
}
