package com.example.emergency_sos_app;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.datepicker.MaterialDatePicker;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SignupStep1Fragment extends Fragment {

    private SignupViewModel viewModel;

    private EditText etFullName;
    private EditText etUsername;
    private EditText etDob;

    private AutoCompleteTextView spinnerGender;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {

        return inflater.inflate(
                R.layout.fragment_signup_step1,
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

        etFullName =
                view.findViewById(R.id.etFullName);

        etUsername =
                view.findViewById(R.id.etUsername);

        etDob =
                view.findViewById(R.id.etDob);

        spinnerGender =
                view.findViewById(R.id.spinnerGender);

        setupGenderSpinner();

        setupDatePicker();

        bindData();

        View btnChangePhoto =
                view.findViewById(
                        R.id.btnChangePhoto
                );

        if (btnChangePhoto != null) {

            btnChangePhoto.setOnClickListener(
                    v -> Toast.makeText(
                            requireContext(),
                            R.string.photo_picker_coming_soon,
                            Toast.LENGTH_SHORT
                    ).show()
            );
        }
    }

    // ==========================================
    // GENDER
    // ==========================================

    private void setupGenderSpinner() {

        String[] genders =
                getResources()
                        .getStringArray(
                                R.array.gender_options
                        );

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_list_item_1,
                        genders
                );

        spinnerGender.setAdapter(adapter);

        spinnerGender.setOnItemClickListener(
                (parent, selectedView, position, id) -> {

                    String gender =
                            adapter.getItem(position);

                    if (gender != null) {

                        viewModel.gender.setValue(
                                gender
                        );
                    }
                }
        );
    }

    // ==========================================
    // DATE OF BIRTH
    // ==========================================

    private void setupDatePicker() {

        etDob.setFocusable(false);
        etDob.setClickable(true);

        etDob.setOnClickListener(v -> {

            MaterialDatePicker<Long>
                    datePicker =
                    MaterialDatePicker.Builder
                            .datePicker()
                            .setTitleText(
                                    R.string.select_dob_title
                            )
                            .setSelection(
                                    MaterialDatePicker
                                            .todayInUtcMilliseconds()
                            )
                            .build();

            datePicker.addOnPositiveButtonClickListener(
                    selection -> {

                        SimpleDateFormat sdf =
                                new SimpleDateFormat(
                                        "dd/MM/yyyy",
                                        Locale.getDefault()
                                );

                        String date =
                                sdf.format(
                                        new Date(selection)
                                );

                        etDob.setText(date);

                        viewModel.dob.setValue(
                                date
                        );
                    }
            );

            datePicker.show(
                    getParentFragmentManager(),
                    "DATE_PICKER"
            );
        });
    }

    // ==========================================
    // RESTORE DATA
    // ==========================================

    private void bindData() {

        if (viewModel.fullName.getValue() != null) {

            etFullName.setText(
                    viewModel.fullName.getValue()
            );
        }

        if (viewModel.username.getValue() != null) {

            etUsername.setText(
                    viewModel.username.getValue()
            );
        }

        if (viewModel.dob.getValue() != null) {

            etDob.setText(
                    viewModel.dob.getValue()
            );
        }

        if (viewModel.gender.getValue() != null) {

            spinnerGender.setText(
                    viewModel.gender.getValue(),
                    false
            );
        }

        etFullName.addTextChangedListener(
                new SimpleTextWatcher(
                        viewModel.fullName
                )
        );

        etUsername.addTextChangedListener(
                new SimpleTextWatcher(
                        viewModel.username
                )
        );
    }

    // ==========================================
    // TEXT WATCHER
    // ==========================================

    private static class SimpleTextWatcher
            implements TextWatcher {

        private final
        androidx.lifecycle.MutableLiveData<String>
                liveData;

        SimpleTextWatcher(
                androidx.lifecycle.MutableLiveData<String>
                        liveData
        ) {

            this.liveData = liveData;
        }

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

            liveData.setValue(
                    s.toString().trim()
            );
        }

        @Override
        public void afterTextChanged(
                Editable s
        ) {
        }
    }
}
