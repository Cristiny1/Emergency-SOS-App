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
import com.example.emergency_sos_app.models.ApiResponse;
import com.example.emergency_sos_app.models.LoginData;
import com.example.emergency_sos_app.models.LoginRequest;
import com.example.emergency_sos_app.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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
                Toast.makeText(this, R.string.google_sign_in_coming, Toast.LENGTH_SHORT).show());

        btnFacebookCircle.setOnClickListener(v ->
                Toast.makeText(this, R.string.facebook_sign_in_coming, Toast.LENGTH_SHORT).show());
    }

//    private void attemptLogin() {
//        String email = etEmail.getText().toString().trim();
//        String password = etPassword.getText().toString().trim();
//
//        if (TextUtils.isEmpty(email)) {
//            etEmail.setError(getString(R.string.email_required));
//            etEmail.requestFocus();
//            return;
//        }
//
//        if (TextUtils.isEmpty(password)) {
//            etPassword.setError(getString(R.string.enter_password));
//            etPassword.requestFocus();
//            return;
//        }
//
//        // ✅ Call backend API instead of checking SharedPreferences
//        loginWithBackend(email, password);
//    }
//
//    // ✅ Add this new method below attemptLogin():
//    private void loginWithBackend(String email, String password) {
//        LoginRequest request = new LoginRequest(email, password);
//
//        RetrofitClient.getApiService().login(request).enqueue(new Callback<ApiResponse<LoginData>>() {
//            @Override
//            public void onResponse(Call<ApiResponse<LoginData>> call, Response<ApiResponse<LoginData>> response) {
//                if (response.isSuccessful() && response.body() != null) {
//                    ApiResponse<LoginData> apiResponse = response.body();
//
//                    if (apiResponse.success && apiResponse.data != null) {
//                        RetrofitClient.saveToken(apiResponse.data.token, apiResponse.data.refreshToken);
//
//                        SharedPreferences sp = getSharedPreferences("sos_profile_prefs", MODE_PRIVATE);
//                        SharedPreferences.Editor editor = sp.edit();
//                        editor.putString("name", apiResponse.data.user.fullName);
//                        editor.putString("email", apiResponse.data.user.email);
//                        if (cbRememberMe.isChecked()) {
//                            editor.putBoolean("remember", true);
//                        }
//                        editor.apply();
//
//                        Toast.makeText(LoginActivity.this, R.string.login_success, Toast.LENGTH_SHORT).show();
//                        Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
//                        intent.putExtra("USER_NAME", apiResponse.data.user.fullName);
//                        startFadeActivity(intent);
//                        finish();
//                    } else {
//                        Toast.makeText(LoginActivity.this, apiResponse.message, Toast.LENGTH_SHORT).show();
//                    }
//                } else {
//                    Toast.makeText(LoginActivity.this, "Server error", Toast.LENGTH_SHORT).show();
//                }
//            }
//
//            @Override
//            public void onFailure(Call<ApiResponse<LoginData>> call, Throwable t) {
//                Toast.makeText(LoginActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
//            }
//        });
//    }



    private void attemptLogin() {

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError(getString(R.string.email_required));
            etEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError(getString(R.string.enter_password));
            etPassword.requestFocus();
            return;
        }

        SharedPreferences sp = getSharedPreferences("sos_profile_prefs", MODE_PRIVATE);

        String savedEmail = sp.getString("email", "admin@gmail.com");
        String savedPassword = sp.getString("password", "123456");
        String savedName = sp.getString("name", "Admin");

        if (email.equals(savedEmail) && password.equals(savedPassword)) {

            SharedPreferences.Editor editor = sp.edit();
            editor.putBoolean("has_account", true); // Set account flag on successful login

            if (cbRememberMe.isChecked()) {
                editor.putBoolean("remember", true);
            } else {
                editor.putBoolean("remember", false);
            }

            editor.apply();

            Toast.makeText(this, R.string.login_success, Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
            intent.putExtra("USER_NAME", savedName);
            startFadeActivity(intent);
            finish();

        } else {
            Toast.makeText(this, R.string.invalid_credentials, Toast.LENGTH_SHORT).show();
        }
    }
}