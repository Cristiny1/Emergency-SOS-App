package com.example.emergency_sos_app;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;

public class SignupStep3Fragment extends Fragment {

    private SignupViewModel viewModel;

    private TextInputEditText etPassword;
    private TextInputEditText etConfirmPassword;

    private CheckBox cbTerms;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {

        return inflater.inflate(
                R.layout.fragment_signup_step3,
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

        etPassword =
                view.findViewById(
                        R.id.etPassword
                );

        etConfirmPassword =
                view.findViewById(
                        R.id.etConfirmPassword
                );

        cbTerms =
                view.findViewById(
                        R.id.cbTerms
                );

        bindData();

        setupListeners();
    }

    // ==========================================
    // RESTORE DATA
    // ==========================================

    private void bindData() {

        if (viewModel.password.getValue() != null) {

            etPassword.setText(
                    viewModel.password.getValue()
            );
        }

        if (viewModel.confirmPassword.getValue() != null) {

            etConfirmPassword.setText(
                    viewModel.confirmPassword.getValue()
            );
        }

        Boolean agreed =
                viewModel.termsAgreed.getValue();

        cbTerms.setChecked(
                agreed != null && agreed
        );
    }

    // ==========================================
    // LISTENERS
    // ==========================================

    private void setupListeners() {

        etPassword.addTextChangedListener(
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

                        viewModel.password.setValue(
                                s.toString()
                        );

                        validatePassword();
                    }

                    @Override
                    public void afterTextChanged(
                            Editable s
                    ) {
                    }
                }
        );

        etConfirmPassword.addTextChangedListener(
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

                        viewModel.confirmPassword
                                .setValue(
                                        s.toString()
                                );

                        validatePasswordMatch();
                    }

                    @Override
                    public void afterTextChanged(
                            Editable s
                    ) {
                    }
                }
        );

        cbTerms.setOnCheckedChangeListener(
                (buttonView, isChecked) ->
                        viewModel.termsAgreed.setValue(
                                isChecked
                        )
        );
    }

    // ==========================================
    // PASSWORD VALIDATION
    // ==========================================

    private void validatePassword() {

        String password =
                etPassword
                        .getText()
                        .toString();

        if (!password.isEmpty()
                && password.length() < 8) {

            etPassword.setError(
                    getString(
                            R.string.password_too_short
                    )
            );

        } else {

            etPassword.setError(null);
        }

        validatePasswordMatch();
    }

    private void validatePasswordMatch() {

        String password =
                etPassword
                        .getText()
                        .toString();

        String confirmPassword =
                etConfirmPassword
                        .getText()
                        .toString();

        if (!confirmPassword.isEmpty()
                && !password.equals(confirmPassword)) {

            etConfirmPassword.setError(
                    getString(
                            R.string.password_mismatch
                    )
            );

        } else {

            etConfirmPassword.setError(null);
        }
    }
}
