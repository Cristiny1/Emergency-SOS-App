package com.example.emergency_sos_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.emergency_sos_app.models.ApiResponse;
import com.example.emergency_sos_app.models.LoginData;
import com.example.emergency_sos_app.network.RetrofitClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.progressindicator.LinearProgressIndicator;

public class SignupActivity extends BaseActivity {

    private ViewPager2 signupViewPager;
    private TextView tvStepIndicator;
    private LinearProgressIndicator signupProgress;
    private Button btnNext;
    private SignupViewModel viewModel;

    private final int totalSteps = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        viewModel = new ViewModelProvider(this).get(SignupViewModel.class);

        initViews();
        setupViewPager();
    }

    private void initViews() {
        tvStepIndicator = findViewById(R.id.tvStepIndicator);
        signupProgress = findViewById(R.id.signupProgress);
        btnNext = findViewById(R.id.btnNext);
        signupViewPager = findViewById(R.id.signupViewPager);

        findViewById(R.id.btnBack).setOnClickListener(v -> {
            if (signupViewPager.getCurrentItem() > 0) {
                signupViewPager.setCurrentItem(signupViewPager.getCurrentItem() - 1);
            } else {
                finish();
            }
        });

        btnNext.setOnClickListener(v -> {
            if (validateCurrentStep()) {
                if (signupViewPager.getCurrentItem() < totalSteps - 1) {
                    signupViewPager.setCurrentItem(signupViewPager.getCurrentItem() + 1);
                } else {
                    completeSignup();
                }
            }
        });
    }

    private void setupViewPager() {
        SignupAdapter adapter = new SignupAdapter(this);
        signupViewPager.setAdapter(adapter);
        signupViewPager.setUserInputEnabled(false);

        signupViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateProgress(position + 1);
                updateNextState(position);
            }
        });

        viewModel.isVerified.observe(this, verified -> updateNextState(signupViewPager.getCurrentItem()));
    }

    private void updateProgress(int step) {
        tvStepIndicator.setText(getString(R.string.step_format, step, totalSteps));
        int progress = (step * 100) / totalSteps;
        signupProgress.setProgress(progress);
        btnNext.setText(step == totalSteps ? R.string.finish_label : R.string.next_label);
    }

    private void updateNextState(int position) {
        // We can choose to leave it enabled and show error toasts on click for better UX
        // or disable it. Let's keep it enabled for Step 2 until code sent, etc.
        // Actually, let's keep it consistent.
    }

    private boolean validateCurrentStep() {
        int current = signupViewPager.getCurrentItem();
        if (current == 0) {
            if (isEmpty(viewModel.fullName) || isEmpty(viewModel.username) || isEmpty(viewModel.dob) || isEmpty(viewModel.gender)) {
                Toast.makeText(this, R.string.error_complete_personal_info, Toast.LENGTH_SHORT).show();
                return false;
            }
        } else if (current == 1) {
            if (!Boolean.TRUE.equals(viewModel.isVerified.getValue())) {
                Toast.makeText(this, R.string.verify_contact_before_next, Toast.LENGTH_SHORT).show();
                return false;
            }
        } else if (current == 2) {
            String p = viewModel.password.getValue();
            String cp = viewModel.confirmPassword.getValue();
            if (p == null || p.length() < 8) {
                Toast.makeText(this, R.string.password_too_short, Toast.LENGTH_SHORT).show();
                return false;
            }
            if (!p.equals(cp)) {
                Toast.makeText(this, R.string.password_mismatch, Toast.LENGTH_SHORT).show();
                return false;
            }
            if (!Boolean.TRUE.equals(viewModel.termsAgreed.getValue())) {
                Toast.makeText(this, R.string.error_password_terms, Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return true;
    }

    private boolean isEmpty(androidx.lifecycle.MutableLiveData<String> liveData) {
        return liveData.getValue() == null || liveData.getValue().trim().isEmpty();
    }

    private void completeSignup() {
        playClickFeedback();

        String email = viewModel.email.getValue() != null ? viewModel.email.getValue().trim() : "";
        String password = viewModel.password.getValue() != null ? viewModel.password.getValue() : "";
        String fullName = viewModel.fullName.getValue() != null ? viewModel.fullName.getValue().trim() : "";

        Map<String, String> payload = new HashMap<>();
        payload.put("email", email);
        payload.put("password", password);
        payload.put("fullName", fullName);

        RetrofitClient.getApiService().register(payload).enqueue(new Callback<ApiResponse<LoginData>>() {
            @Override
            public void onResponse(Call<ApiResponse<LoginData>> call, Response<ApiResponse<LoginData>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success && response.body().data != null) {
                    LoginData data = response.body().data;
                    RetrofitClient.saveToken(data.token, data.refreshToken);
                    saveLocalProfile(data.user != null ? data.user.fullName : fullName, email, password);
                    Toast.makeText(SignupActivity.this, R.string.account_created_success, Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(SignupActivity.this, DashboardActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                    return;
                }

                saveLocalProfile(fullName, email, password);
                Toast.makeText(SignupActivity.this, R.string.account_created_success, Toast.LENGTH_LONG).show();
                Intent intent = new Intent(SignupActivity.this, DashboardActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(Call<ApiResponse<LoginData>> call, Throwable t) {
                saveLocalProfile(fullName, email, password);
                Toast.makeText(SignupActivity.this, R.string.account_created_success, Toast.LENGTH_LONG).show();
                Intent intent = new Intent(SignupActivity.this, DashboardActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    private void saveLocalProfile(String fullName, String email, String password) {
        String hashedPassword = hashPassword(password);
        getSharedPreferences("sos_profile_prefs", MODE_PRIVATE).edit()
                .putString("name", fullName)
                .putString("username", viewModel.username.getValue())
                .putString("dob", viewModel.dob.getValue())
                .putString("gender", viewModel.gender.getValue())
                .putString("email", email)
                .putString("phone", viewModel.phone.getValue())
                .putString("password", hashedPassword)
                .putBoolean("remember", true)
                .putBoolean("has_account", true)
                .apply();
    }

    private String hashPassword(String password) {
        if (password == null || password.isEmpty()) {
            return "";
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                String hexValue = Integer.toHexString(0xff & b);
                if (hexValue.length() == 1) {
                    hex.append('0');
                }
                hex.append(hexValue);
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            return password;
        }
    }

    private static class SignupAdapter extends FragmentStateAdapter {
        public SignupAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0: return new SignupStep1Fragment();
                case 1: return new SignupStep2Fragment();
                case 2: return new SignupStep3Fragment();
                default: return new SignupStep1Fragment();
            }
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }
}
