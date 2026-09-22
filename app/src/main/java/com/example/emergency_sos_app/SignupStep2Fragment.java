package com.example.emergency_sos_app;

import android.content.Context;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.util.Locale;

public class SignupStep2Fragment extends Fragment {

    private SignupViewModel viewModel;

    private EditText etEmail;
    private EditText etPhone;

    private EditText[] otpBoxes;

    private MaterialButton btnSendOtp;
    private MaterialButton btnResend;

    private MaterialButtonToggleGroup channelToggle;

    private View layoutOtp;

    private ImageView ivOtpStatus;

    private TextView tvOtpSubtitle;
    private TextView tvResendTimer;

    private CountDownTimer countDownTimer;

    private OtpManager.DeliveryMethod deliveryMethod =
            OtpManager.DeliveryMethod.EMAIL;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {

        return inflater.inflate(
                R.layout.fragment_signup_step2,
                container,
                false
        );
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {

        super.onViewCreated(
                view,
                savedInstanceState
        );

        viewModel =
                new ViewModelProvider(
                        requireActivity()
                ).get(SignupViewModel.class);

        etEmail =
                view.findViewById(R.id.etEmail);

        etPhone =
                view.findViewById(R.id.etPhone);

        otpBoxes = new EditText[]{
                view.findViewById(R.id.otp1),
                view.findViewById(R.id.otp2),
                view.findViewById(R.id.otp3),
                view.findViewById(R.id.otp4),
                view.findViewById(R.id.otp5),
                view.findViewById(R.id.otp6)
        };

        btnSendOtp =
                view.findViewById(R.id.btnSendOtp);

        btnResend =
                view.findViewById(R.id.btnResend);

        channelToggle =
                view.findViewById(R.id.channelToggle);

        layoutOtp =
                view.findViewById(R.id.layoutOtp);

        ivOtpStatus =
                view.findViewById(R.id.ivOtpStatus);

        tvOtpSubtitle =
                view.findViewById(R.id.tvOtpSubtitle);

        tvResendTimer =
                view.findViewById(R.id.tvResendTimer);

        btnSendOtp.setOnClickListener(
                v -> sendOtp()
        );

        btnResend.setOnClickListener(
                v -> sendOtp()
        );

        setupChannelToggle();

        setupOtpInputs();

        bindData();
    }

    // ==========================================
    // EMAIL / SMS TOGGLE
    // ==========================================

    private void setupChannelToggle() {

        channelToggle.addOnButtonCheckedListener(
                (group, checkedId, isChecked) -> {

                    if (!isChecked) {
                        return;
                    }

                    if (checkedId == R.id.btnSms) {

                        deliveryMethod =
                                OtpManager.DeliveryMethod.SMS;

                    } else {

                        deliveryMethod =
                                OtpManager.DeliveryMethod.EMAIL;
                    }

                    // New channel = new verification.
                    viewModel.isVerified.setValue(false);

                    clearOtpBoxes();

                    ivOtpStatus.setVisibility(
                            View.GONE
                    );

                    layoutOtp.setVisibility(
                            View.GONE
                    );
                }
        );
    }

    // ==========================================
    // SEND OTP
    // ==========================================

    private void sendOtp() {

        boolean emailSelected =
                deliveryMethod ==
                        OtpManager.DeliveryMethod.EMAIL;

        EditText selectedField =
                emailSelected
                        ? etEmail
                        : etPhone;

        String destination =
                selectedField
                        .getText()
                        .toString()
                        .trim();

        if (TextUtils.isEmpty(destination)) {

            selectedField.setError(
                    getString(
                            emailSelected
                                    ? R.string.email_required
                                    : R.string.phone_required
                    )
            );

            return;
        }

        // Email validation.
        if (emailSelected &&
                !android.util.Patterns
                        .EMAIL_ADDRESS
                        .matcher(destination)
                        .matches()) {

            selectedField.setError(
                    getString(
                            R.string.invalid_email
                    )
            );

            return;
        }

        // Simple phone validation.
        if (!emailSelected &&
                destination.length() < 8) {

            selectedField.setError(
                    getString(
                            R.string.invalid_phone
                    )
            );

            return;
        }

        viewModel.isVerified.setValue(false);

        // Send OTP.
        OtpManager.getInstance().sendOtp(
                requireContext(),
                destination,
                deliveryMethod
        );

        layoutOtp.setVisibility(
                View.VISIBLE
        );

        tvOtpSubtitle.setText(
                getString(
                        R.string.otp_sent_to,
                        destination
                )
        );

        clearOtpBoxes();

        ivOtpStatus.setVisibility(
                View.GONE
        );

        startResendTimer();

        otpBoxes[0].requestFocus();
    }

    // ==========================================
    // OTP INPUT
    // ==========================================

    private void setupOtpInputs() {

        for (int i = 0;
             i < otpBoxes.length;
             i++) {

            final int index = i;

            otpBoxes[i].setInputType(
                    android.text.InputType
                            .TYPE_CLASS_NUMBER
            );

            otpBoxes[i].addTextChangedListener(
                    new TextWatcher() {

                        @Override
                        public void beforeTextChanged(
                                CharSequence s,
                                int start,
                                int count,
                                int after
                        ) {
                        }

                        @Override
                        public void onTextChanged(
                                CharSequence s,
                                int start,
                                int before,
                                int count
                        ) {
                        }

                        @Override
                        public void afterTextChanged(
                                Editable editable
                        ) {

                            if (editable.length() > 1) {

                                editable.delete(
                                        1,
                                        editable.length()
                                );
                            }

                            if (editable.length() == 1 &&
                                    index < otpBoxes.length - 1) {

                                otpBoxes[index + 1]
                                        .requestFocus();

                            } else if (
                                    editable.length() == 0 &&
                                            index > 0
                            ) {

                                otpBoxes[index - 1]
                                        .requestFocus();
                            }

                            if (index ==
                                    otpBoxes.length - 1 &&
                                    editable.length() == 1) {

                                verifyOtp();
                            }
                        }
                    }
            );
        }
    }

    // ==========================================
    // VERIFY OTP
    // ==========================================

    private void verifyOtp() {

        StringBuilder code =
                new StringBuilder();

        for (EditText box : otpBoxes) {

            String value =
                    box.getText()
                            .toString()
                            .trim();

            if (value.isEmpty()) {
                return;
            }

            code.append(value);
        }

        if (code.length() != 6) {
            return;
        }

        boolean verified =
                OtpManager.getInstance()
                        .verifyOtp(
                                code.toString()
                        );

        if (verified) {

            viewModel.isVerified.setValue(
                    true
            );

            ivOtpStatus.setVisibility(
                    View.VISIBLE
            );

            for (EditText box : otpBoxes) {
                box.setEnabled(false);
            }

            if (countDownTimer != null) {
                countDownTimer.cancel();
            }

            btnResend.setVisibility(
                    View.GONE
            );

            tvResendTimer.setVisibility(
                    View.GONE
            );

            Vibrator vibrator =
                    (Vibrator)
                            requireContext()
                                    .getSystemService(
                                            Context.VIBRATOR_SERVICE
                                    );

            if (vibrator != null) {

                vibrator.vibrate(
                        VibrationEffect
                                .createOneShot(
                                        80,
                                        VibrationEffect
                                                .DEFAULT_AMPLITUDE
                                )
                );
            }

            Toast.makeText(
                    requireContext(),
                    R.string.verify_code_success,
                    Toast.LENGTH_SHORT
            ).show();

        } else {

            viewModel.isVerified.setValue(
                    false
            );

            layoutOtp.startAnimation(
                    android.view.animation
                            .AnimationUtils
                            .loadAnimation(
                                    requireContext(),
                                    R.anim.shake
                            )
            );

            clearOtpBoxes();

            otpBoxes[0].requestFocus();

            ivOtpStatus.setVisibility(
                    View.GONE
            );

            Toast.makeText(
                    requireContext(),
                    R.string.invalid_otp_code,
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // ==========================================
    // CLEAR OTP
    // ==========================================

    private void clearOtpBoxes() {

        for (EditText box : otpBoxes) {

            box.setText("");
            box.setEnabled(true);
        }
    }

    // ==========================================
    // RESEND TIMER
    // ==========================================

    private void startResendTimer() {

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        btnResend.setVisibility(
                View.GONE
        );

        tvResendTimer.setVisibility(
                View.VISIBLE
        );

        countDownTimer =
                new CountDownTimer(
                        60000,
                        1000
                ) {

                    @Override
                    public void onTick(
                            long millisUntilFinished
                    ) {

                        tvResendTimer.setText(
                                String.format(
                                        Locale.getDefault(),
                                        getString(
                                                R.string.resend_in
                                        ),
                                        millisUntilFinished / 1000
                                )
                        );
                    }

                    @Override
                    public void onFinish() {

                        tvResendTimer.setVisibility(
                                View.GONE
                        );

                        btnResend.setVisibility(
                                View.VISIBLE
                        );
                    }
                }.start();
    }

    // ==========================================
    // RESTORE DATA
    // ==========================================

    private void bindData() {

        if (viewModel.email.getValue() != null) {

            etEmail.setText(
                    viewModel.email.getValue()
            );
        }

        if (viewModel.phone.getValue() != null) {

            etPhone.setText(
                    viewModel.phone.getValue()
            );
        }

        etEmail.addTextChangedListener(
                new SimpleTextWatcher(
                        this,
                        viewModel.email
                )
        );

        etPhone.addTextChangedListener(
                new SimpleTextWatcher(
                        this,
                        viewModel.phone
                )
        );
    }

    private static class SimpleTextWatcher
            implements TextWatcher {

        private final SignupStep2Fragment fragment;
        private final androidx.lifecycle.MutableLiveData<String> liveData;

        SimpleTextWatcher(
                SignupStep2Fragment fragment,
                androidx.lifecycle.MutableLiveData<String> liveData
        ) {
            this.fragment = fragment;
            this.liveData = liveData;
        }

        @Override
        public void beforeTextChanged(
                CharSequence s,
                int start, int count, int after
        ) {
        }

        @Override
        public void onTextChanged(
                CharSequence s,
                int start, int before, int count
        ) {

            liveData.setValue(
                    s.toString().trim()
            );

            // Reset verification status if user changes contact info
            fragment.viewModel.isVerified.setValue(false);
            
            if (fragment.layoutOtp != null) {
                fragment.layoutOtp.setVisibility(View.GONE);
            }
            if (fragment.ivOtpStatus != null) {
                fragment.ivOtpStatus.setVisibility(View.GONE);
            }
        }

        @Override
        public void afterTextChanged(
                Editable s
        ) {
        }
    }

    @Override
    public void onDestroyView() {

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        super.onDestroyView();
    }
}
