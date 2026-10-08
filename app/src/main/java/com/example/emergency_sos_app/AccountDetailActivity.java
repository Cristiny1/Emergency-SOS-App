package com.example.emergency_sos_app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.example.emergency_sos_app.network.RetrofitClient;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import de.hdodenhof.circleimageview.CircleImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AccountDetailActivity extends BaseActivity {

    private static final String PREFS_NAME = "sos_profile_prefs";
    
    private EditText etFullName, etEmail, etPhone, etUsername, etDob;
    private Spinner spinnerGender;
    private CircleImageView ivAccountProfile;
    private SharedPreferences prefs;
    private boolean isEditMode = false;
    private TextView btnSave;
    private com.google.android.material.card.MaterialCardView btnChangePhoto;

    private ActivityResultLauncher<String> photoPickerLauncher;
    private final ExecutorService imageIoExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_detail);

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        initViews();
        setupPhotoPicker();
        setupGenderSpinner();
        setupDatePicker();
        loadAccountData();
        setEditMode(false); // Start in view mode

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        btnSave.setOnClickListener(v -> {
            playClickFeedback();
            if (isEditMode) {
                saveAccountData();
            } else {
                setEditMode(true);
            }
        });
        
        btnChangePhoto.setOnClickListener(v -> {
            if (isEditMode) {
                playClickFeedback();
                photoPickerLauncher.launch("image/*");
            }
        });

        findViewById(R.id.rowChangePassword).setOnClickListener(v -> {
            playClickFeedback();
            startFadeActivity(new Intent(this, ForgotPasswordActivity.class));
        });

        findViewById(R.id.rowDeleteAccount).setOnClickListener(v -> {
            playClickFeedback();
            showDeleteConfirmation();
        });
    }

    private void initViews() {
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etUsername = findViewById(R.id.etUsername);
        etDob = findViewById(R.id.etDob);
        spinnerGender = findViewById(R.id.spinnerGender);
        ivAccountProfile = findViewById(R.id.ivAccountProfile);
        btnSave = findViewById(R.id.btnSave);
        btnChangePhoto = findViewById(R.id.btnChangePhoto);
    }

    private void setupGenderSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.gender_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGender.setAdapter(adapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        imageIoExecutor.shutdownNow();
    }

    private void setupDatePicker() {
        etDob.setOnClickListener(v -> {
            if (!isEditMode) return;
            
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText(R.string.select_dob_title)
                    .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                    .build();

            datePicker.addOnPositiveButtonClickListener(selection -> {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                String dateString = sdf.format(new Date(selection));
                etDob.setText(dateString);
            });

            datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
        });
    }

    private void setEditMode(boolean edit) {
        this.isEditMode = edit;
        
        etFullName.setEnabled(edit);
        etEmail.setEnabled(edit);
        etPhone.setEnabled(edit);
        etUsername.setEnabled(edit);
        etDob.setEnabled(edit); // Controlled by click listener too
        spinnerGender.setEnabled(edit);
        
        if (edit) {
            btnSave.setText(R.string.save_label);
            btnSave.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_check, 0, 0, 0);
            androidx.core.widget.TextViewCompat.setCompoundDrawableTintList(btnSave, android.content.res.ColorStateList.valueOf(getColor(R.color.primary)));
            btnChangePhoto.setVisibility(View.VISIBLE);
        } else {
            btnSave.setText(R.string.edit_label);
            btnSave.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_edit, 0, 0, 0);
            androidx.core.widget.TextViewCompat.setCompoundDrawableTintList(btnSave, android.content.res.ColorStateList.valueOf(getColor(R.color.primary)));
            btnChangePhoto.setVisibility(View.GONE);
        }
        
        // Visual feedback
        float alpha = edit ? 1.0f : 0.6f;
        etFullName.setAlpha(alpha);
        etEmail.setAlpha(alpha);
        etPhone.setAlpha(alpha);
        etUsername.setAlpha(alpha);
        etDob.setAlpha(alpha);
        spinnerGender.setAlpha(alpha);
    }

    private void setupPhotoPicker() {
        photoPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri == null) return;

                    imageIoExecutor.execute(() -> {
                        File savedFile = copyImageToInternalStorage(uri);
                        if (savedFile == null) return;

                        prefs.edit().putString("profile_photo_path", savedFile.getAbsolutePath()).apply();

                        Bitmap bitmap = decodeBitmapFromFile(savedFile);
                        runOnUiThread(() -> {
                            if (bitmap != null) {
                                ivAccountProfile.setImageBitmap(bitmap);
                            } else {
                                ivAccountProfile.setImageResource(R.drawable.ic_personal_white);
                            }
                            vibrate(40);
                        });
                    });
                }
        );
    }

    private Bitmap decodeBitmapFromFile(File photoFile) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(photoFile.getAbsolutePath(), options);
            options.inSampleSize = calculateInSampleSize(options, 512, 512);
            options.inJustDecodeBounds = false;
            return BitmapFactory.decodeFile(photoFile.getAbsolutePath(), options);
        } catch (Exception e) {
            return null;
        }
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int inSampleSize = 1;
        final int height = options.outHeight;
        final int width = options.outWidth;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private File copyImageToInternalStorage(Uri sourceUri) {
        try (InputStream in = getContentResolver().openInputStream(sourceUri)) {
            if (in == null) return null;
            File outFile = new File(getFilesDir(), "profile_photo.jpg");
            try (FileOutputStream out = new FileOutputStream(outFile)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }
            return outFile;
        } catch (Exception e) {
            return null;
        }
    }

    private void loadAccountData() {
        etFullName.setText(prefs.getString("name", ""));
        etEmail.setText(prefs.getString("email", ""));
        etPhone.setText(prefs.getString("phone", ""));
        etUsername.setText(prefs.getString("username", ""));
        etDob.setText(prefs.getString("dob", ""));

        String gender = prefs.getString("gender", "");
        if (!gender.isEmpty()) {
            ArrayAdapter adapter = (ArrayAdapter) spinnerGender.getAdapter();
            int pos = adapter.getPosition(gender);
            if (pos >= 0) spinnerGender.setSelection(pos);
        }

        String photoPath = prefs.getString("profile_photo_path", null);
        if (photoPath != null) {
            File photoFile = new File(photoPath);
            if (photoFile.exists()) {
                imageIoExecutor.execute(() -> {
                    Bitmap bitmap = decodeBitmapFromFile(photoFile);
                    runOnUiThread(() -> {
                        if (bitmap != null) {
                            ivAccountProfile.setImageBitmap(bitmap);
                        } else {
                            ivAccountProfile.setImageResource(R.drawable.ic_personal_white);
                        }
                    });
                });
            }
        } else {
            ivAccountProfile.setImageResource(R.drawable.ic_personal_white);
        }
    }

    private void saveAccountData() {
        String name = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String user = etUsername.getText().toString().trim();
        String dob = etDob.getText().toString().trim();
        String gender = spinnerGender.getSelectedItem() != null ? spinnerGender.getSelectedItem().toString() : "";

        if (name.isEmpty()) {
            etFullName.setError(getString(R.string.name_required));
            return;
        }

        playClickFeedback();
        prefs.edit()
                .putString("name", name)
                .putString("email", email)
                .putString("phone", phone)
                .putString("username", user)
                .putString("dob", dob)
                .putString("gender", gender)
                .apply();

        Toast.makeText(this, R.string.profile_updated_success, Toast.LENGTH_SHORT).show();
        setEditMode(false); // Switch back to view mode after saving
    }

    private void showDeleteConfirmation() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_account_label)
                .setMessage(R.string.delete_account_confirm)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    clearLocalAccountData();
                    Toast.makeText(this, R.string.account_deleted_success, Toast.LENGTH_SHORT).show();
                    
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void clearLocalAccountData() {
        String photoPath = prefs.getString("profile_photo_path", null);
        prefs.edit().clear().apply();

        getSharedPreferences("sos_history_prefs", MODE_PRIVATE).edit().clear().apply();
        getSharedPreferences("family_circle_prefs", MODE_PRIVATE).edit().clear().apply();
        getSharedPreferences("safety_calendar_prefs", MODE_PRIVATE).edit().clear().apply();
        getSharedPreferences("translation_cache_prefs", MODE_PRIVATE).edit().clear().apply();
        RetrofitClient.clearTokens();

        if (photoPath != null) {
            File photoFile = new File(photoPath);
            if (photoFile.isFile()) photoFile.delete();
        }

        android.app.NotificationManager notificationManager = getSystemService(android.app.NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.cancel(101);
            notificationManager.cancel(202);
        }
    }
}
