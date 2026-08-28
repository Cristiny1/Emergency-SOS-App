package com.example.emergency_sos_app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;

public class OtpVerificationActivity extends BaseActivity {

    private EditText[] otpBoxes;
    private View layoutInputs;
    private ProgressBar pbVerifying;
    private ImageView ivSuccess;
    private TextView tvOtpSubtitle, tvResendTimer;
    private TextView btnResend, btnTryAnotherWay;
    private Button btnVerify;
    private CountDownTimer countDownTimer;
    private String destination, name, email, password;
    private boolean isResetFlow = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_verification);

        // Get data from intent
        Intent intent = getIntent();
        destination = intent.getStringExtra("DESTINATION");
        if (destination == null) destination = intent.getStringExtra("PHONE"); // Fallback for signup
        
        name = intent.getStringExtra("NAME");
        email = intent.getStringExtra("EMAIL");
        password = intent.getStringExtra("PASSWORD");
        isResetFlow = intent.getBooleanExtra("IS_RESET_FLOW", false);

        initViews();
        setupOtpInputs();
        startResendTimer();

        tvOtpSubtitle.setText(String.format(getString(R.string.otp_subtitle), destination));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnVerify.setOnClickListener(v -> verifyOtp());
        
        btnResend.setOnClickListener(v -> {
            // Determine method based on destination format (simple check)
            OtpManager.DeliveryMethod method = destination.contains("@") ? 
                    OtpManager.DeliveryMethod.EMAIL : OtpManager.DeliveryMethod.SMS;
            OtpManager.getInstance().sendOtp(this, destination, method);
            startResendTimer();
            btnResend.setVisibility(View.GONE);
            tvResendTimer.setVisibility(View.VISIBLE);
        });

        btnTryAnotherWay.setOnClickListener(v -> finish()); // Go back to selection
    }

    private void initViews() {
        tvOtpSubtitle = findViewById(R.id.tvOtpSubtitle);
        tvResendTimer = findViewById(R.id.tvResendTimer);
        btnResend = findViewById(R.id.btnResend);
        btnTryAnotherWay = findViewById(R.id.btnTryAnotherWay);
        btnVerify = findViewById(R.id.btnVerify);
        pbVerifying = findViewById(R.id.pbVerifying);
        ivSuccess = findViewById(R.id.ivSuccess);
        layoutInputs = findViewById(R.id.layoutOtpInputs);

        otpBoxes = new EditText[]{
                findViewById(R.id.otp1),
                findViewById(R.id.otp2),
                findViewById(R.id.otp3),
                findViewById(R.id.otp4),
                findViewById(R.id.otp5),
                findViewById(R.id.otp6)
        };
    }

    private void setupOtpInputs() {
        for (int i = 0; i < otpBoxes.length; i++) {
            final int index = i;
            otpBoxes[i].addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() == 1) {
                        animateBox(otpBoxes[index]);
                        if (index < otpBoxes.length - 1) {
                            otpBoxes[index + 1].requestFocus();
                        } else {
                            // Automatic verification on last digit
                            verifyOtp();
                        }
                    } else if (s.length() == 0 && index > 0) {
                        otpBoxes[index - 1].requestFocus();
                    }
                }
            });
        }
    }

    private void animateBox(View v) {
        v.animate().scaleX(1.15f).scaleY(1.15f).setDuration(100)
                .withEndAction(() -> v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start())
                .start();
    }

    private void startResendTimer() {
        if (countDownTimer != null) countDownTimer.cancel();

        countDownTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                tvResendTimer.setText(String.format(Locale.getDefault(), 
                    getString(R.string.resend_in), millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                tvResendTimer.setVisibility(View.GONE);
                btnResend.setVisibility(View.VISIBLE);
            }
        }.start();
    }

    private void verifyOtp() {
        StringBuilder codeBuilder = new StringBuilder();
        for (EditText box : otpBoxes) {
            codeBuilder.append(box.getText().toString());
        }
        String enteredCode = codeBuilder.toString();

        if (enteredCode.length() < 6) return;

        showLoading(true);

        // Simulate network delay for premium feel
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (OtpManager.getInstance().verifyOtp(enteredCode)) {
                onVerificationSuccess();
            } else {
                onVerificationError();
            }
        }, 1500);
    }

    private void showLoading(boolean show) {
        pbVerifying.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            btnVerify.setVisibility(View.INVISIBLE);
            btnTryAnotherWay.setVisibility(View.INVISIBLE);
        } else {
            btnVerify.setVisibility(View.VISIBLE);
            btnTryAnotherWay.setVisibility(View.VISIBLE);
        }
        for (EditText box : otpBoxes) box.setEnabled(!show);
    }

    private void onVerificationSuccess() {
        showLoading(false);
        btnVerify.setVisibility(View.GONE);
        btnTryAnotherWay.setVisibility(View.GONE);
        
        ivSuccess.setVisibility(View.VISIBLE);
        ivSuccess.setScaleX(0);
        ivSuccess.setScaleY(0);
        ivSuccess.animate().scaleX(1.2f).scaleY(1.2f).setDuration(300)
                .withEndAction(() -> ivSuccess.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100)
                        .withEndAction(() -> {
                            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                                if (isResetFlow) {
                                    startActivity(new Intent(this, ResetPasswordActivity.class));
                                } else {
                                    saveUserAndFinish();
                                }
                                finish();
                            }, 600);
                        }).start())
                .start();
    }

    private void onVerificationError() {
        showLoading(false);
        Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);
        layoutInputs.startAnimation(shake);
        Toast.makeText(this, getString(R.string.invalid_otp), Toast.LENGTH_SHORT).show();
        
        // Clear inputs on error
        for (EditText box : otpBoxes) box.setText("");
        otpBoxes[0].requestFocus();
    }

    private void saveUserAndFinish() {
        SharedPreferences sp = getSharedPreferences("sos_profile_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();

        editor.putString("name", name);
        editor.putString("email", email);
        editor.putString("password", password);
        editor.putString("phone", destination);
        editor.putBoolean("remember", true); // Auto login after signup

        editor.apply();

        Toast.makeText(this, "Verification Successful!", Toast.LENGTH_LONG).show();

        Intent intent = new Intent(OtpVerificationActivity.this, DashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
    }
}
