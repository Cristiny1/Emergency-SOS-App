package com.example.emergency_sos_app;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class ProfileActivity extends BaseActivity {

    private static final String PREFS_NAME = "sos_profile_prefs";
    private static final String TAG = "ProfileActivity";

    private TextView tvMedName, tvMedAge, tvMedLanguage, tvBloodGroup;
    private TextView tvPregnancy, tvMedications, tvAllergies, tvContacts, tvConditions;
    private ImageView ivMedPhoto;
    private Vibrator vibrator;

    private ActivityResultLauncher<String> photoPickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        initViews();
        setupPhotoPicker();
        loadMedicalId();
        setupEdgeToEdge();
        NavigationHelper.setup(this, R.id.nav_profile);

        findViewById(R.id.btnBack).setOnClickListener(v -> {
            vibrate(20);
            finish();
        });

        // Set up card interactions
        setupCardInteraction(findViewById(R.id.cardPhotoInfo), () -> photoPickerLauncher.launch("image/*"));
        setupCardInteraction(findViewById(R.id.cardBlood), this::showBloodGroupPicker);
        setupCardInteraction(findViewById(R.id.rowPregnancy), this::showPregnancyPicker);
        setupCardInteraction(findViewById(R.id.cardMedications), () -> showSingleFieldDialog(R.string.medications_label, tvMedications, "medications", false));
        setupCardInteraction(findViewById(R.id.cardAllergies), () -> showSingleFieldDialog(R.string.allergies_label, tvAllergies, "allergies", false));
        setupCardInteraction(findViewById(R.id.cardContacts), () -> showSingleFieldDialog(R.string.emergency_contacts_label, tvContacts, "contacts", false));
        setupCardInteraction(findViewById(R.id.cardConditions), () -> showSingleFieldDialog(R.string.conditions_label, tvConditions, "conditions", false));

        // Edit text listeners
        findViewById(R.id.editPhotoInfo).setOnClickListener(v -> { vibrate(20); showMultiFieldDialog(); });
        findViewById(R.id.editBlood).setOnClickListener(v -> { vibrate(20); showBloodGroupPicker(); });
        findViewById(R.id.editPregnancy).setOnClickListener(v -> { vibrate(20); showPregnancyPicker(); });
        findViewById(R.id.editMedications).setOnClickListener(v -> { vibrate(20); showSingleFieldDialog(R.string.medications_label, tvMedications, "medications", false); });
        findViewById(R.id.editAllergies).setOnClickListener(v -> { vibrate(20); showSingleFieldDialog(R.string.allergies_label, tvAllergies, "allergies", false); });
        findViewById(R.id.editContacts).setOnClickListener(v -> { vibrate(20); showSingleFieldDialog(R.string.emergency_contacts_label, tvContacts, "contacts", false); });
        findViewById(R.id.editConditions).setOnClickListener(v -> { vibrate(20); showSingleFieldDialog(R.string.conditions_label, tvConditions, "conditions", false); });
    }

    private void initViews() {
        tvMedName = findViewById(R.id.tvMedName);
        tvMedAge = findViewById(R.id.tvMedAge);
        tvMedLanguage = findViewById(R.id.tvMedLanguage);
        tvBloodGroup = findViewById(R.id.tvBloodGroup);
        tvPregnancy = findViewById(R.id.tvPregnancy);
        tvMedications = findViewById(R.id.tvMedications);
        tvAllergies = findViewById(R.id.tvAllergies);
        tvContacts = findViewById(R.id.tvContacts);
        tvConditions = findViewById(R.id.tvConditions);
        ivMedPhoto = findViewById(R.id.ivMedPhoto);
    }

    private void setupPhotoPicker() {
        photoPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri == null) return;
                    File savedFile = copyImageToInternalStorage(uri);
                    if (savedFile != null) {
                        ivMedPhoto.setImageURI(Uri.fromFile(savedFile));
                        getPrefs().edit().putString("profile_photo_path", savedFile.getAbsolutePath()).apply();
                        vibrate(40);
                        Toast.makeText(this, getString(R.string.photo_updated), Toast.LENGTH_SHORT).show();
                    }
                }
        );
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

    private void setupCardInteraction(View view, Runnable action) {
        if (view == null) return;
        view.setOnClickListener(v -> action.run());
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.98f).scaleY(0.98f).setDuration(100).start();
                    vibrate(10);
                    break;
                case MotionEvent.ACTION_UP:
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).setInterpolator(new OvershootInterpolator()).start();
                    vibrate(20);
                    v.performClick();
                    break;
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).setInterpolator(new OvershootInterpolator()).start();
                    break;
            }
            return true;
        });
    }

    private void vibrate(long millis) {
        if (vibrator == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(millis);
        }
    }

    private SharedPreferences getPrefs() {
        return getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private void loadMedicalId() {
        SharedPreferences prefs = getPrefs();
        String addLabel = getString(R.string.add_label);

        updateField(tvMedName, prefs.getString("name", "Danny Rico"), false);
        updateField(tvMedAge, prefs.getString("age", "41"), false);
        updateField(tvMedLanguage, prefs.getString("language", "Spanish · Español"), false);
        updateField(tvBloodGroup, prefs.getString("blood_group", "O+"), false);
        updateField(tvPregnancy, prefs.getString("pregnancy", addLabel), true);
        updateField(tvMedications, prefs.getString("medications", "Lisinopril (10mg) by mouth"), false);
        updateField(tvAllergies, prefs.getString("allergies", "Peanuts"), false);
        updateField(tvContacts, prefs.getString("contacts", "Ashley Rico"), false);
        updateField(tvConditions, prefs.getString("conditions", "Hypertension"), false);

        String photoPath = prefs.getString("profile_photo_path", null);
        if (photoPath != null) {
            File photoFile = new File(photoPath);
            if (photoFile.exists()) ivMedPhoto.setImageURI(Uri.fromFile(photoFile));
        }
    }

    private void updateField(TextView view, String value, boolean isAddRed) {
        if (view == null) return;
        view.setText(value);
        if (isAddRed && value.equals(getString(R.string.add_label))) {
            view.setTextColor(getColor(R.color.sos_red));
        } else {
            view.setTextColor(getColor(R.color.text_dark));
        }
    }

    private void showBloodGroupPicker() {
        String[] options = getResources().getStringArray(R.array.blood_groups);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.blood_group_label)
                .setItems(options, (dialog, which) -> {
                    String selected = options[which];
                    updateField(tvBloodGroup, selected, false);
                    getPrefs().edit().putString("blood_group", selected).apply();
                    vibrate(30);
                })
                .show();
    }

    private void showPregnancyPicker() {
        String[] options = getResources().getStringArray(R.array.pregnancy_options);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.pregnancy_label)
                .setItems(options, (dialog, which) -> {
                    String selected = options[which];
                    updateField(tvPregnancy, selected, true);
                    getPrefs().edit().putString("pregnancy", selected).apply();
                    vibrate(30);
                })
                .show();
    }

    private void showSingleFieldDialog(int titleRes, TextView targetView, String prefKey, boolean allowEmptyAsAdd) {
        String title = getString(titleRes);
        String addLabel = getString(R.string.add_label);

        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        String current = targetView.getText().toString();
        input.setText(current.equals(addLabel) ? "" : current);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        int m = dpToPx(24);
        params.setMargins(m, dpToPx(16), m, dpToPx(16));
        input.setLayoutParams(params);
        container.addView(input);

        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.edit_field_title, title))
                .setView(container)
                .setPositiveButton(R.string.save_label, (dialog, which) -> {
                    vibrate(30);
                    String value = input.getText().toString().trim();
                    if (value.isEmpty() && allowEmptyAsAdd) value = addLabel;
                    updateField(targetView, value, allowEmptyAsAdd);
                    getPrefs().edit().putString(prefKey, value).apply();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showMultiFieldDialog() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int p = dpToPx(24);
        container.setPadding(p, dpToPx(16), p, 0);

        EditText inputName = new EditText(this);
        inputName.setHint(R.string.full_name_field);
        inputName.setText(tvMedName.getText().toString());
        container.addView(inputName, fieldParams());

        EditText inputAge = new EditText(this);
        inputAge.setHint(R.string.age_hint);
        inputAge.setInputType(InputType.TYPE_CLASS_NUMBER); // Numeric Lock
        inputAge.setText(tvMedAge.getText().toString().replaceAll("[^0-9]", ""));
        container.addView(inputAge, fieldParams());

        EditText inputLanguage = new EditText(this);
        inputLanguage.setHint(R.string.language_hint);
        inputLanguage.setText(tvMedLanguage.getText().toString());
        container.addView(inputLanguage, fieldParams());

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.edit_personal_info_desc)
                .setView(container)
                .setPositiveButton(R.string.save_label, (dialog, which) -> {
                    vibrate(30);
                    String name = inputName.getText().toString().trim();
                    String age = inputAge.getText().toString().trim();
                    String lang = inputLanguage.getText().toString().trim();

                    if (!name.isEmpty()) updateField(tvMedName, name, false);
                    if (!age.isEmpty()) updateField(tvMedAge, age, false);
                    if (!lang.isEmpty()) updateField(tvMedLanguage, lang, false);

                    getPrefs().edit()
                            .putString("name", tvMedName.getText().toString())
                            .putString("age", tvMedAge.getText().toString())
                            .putString("language", tvMedLanguage.getText().toString())
                            .apply();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private LinearLayout.LayoutParams fieldParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dpToPx(8);
        return params;
    }

    private int dpToPx(float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private void setupEdgeToEdge() {
        View root = findViewById(R.id.scrollMedicalId);
        View backButton = findViewById(R.id.btnBack);
        View bottomNav = findViewById(R.id.bottomNavigation);
        View topLayout = (View) backButton.getParent();

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            int navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;

            // Apply padding to the top layout to avoid status bar overlap
            if (topLayout != null) {
                topLayout.setPadding(topLayout.getPaddingLeft(), statusBarHeight, topLayout.getPaddingRight(), topLayout.getPaddingBottom());
            }

            // Apply margin to bottom navigation
            if (bottomNav != null) {
                androidx.constraintlayout.widget.ConstraintLayout.LayoutParams lp = 
                    (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) bottomNav.getLayoutParams();
                lp.bottomMargin = dpToPx(16) + navBarHeight;
                bottomNav.setLayoutParams(lp);
            }

            return WindowInsetsCompat.CONSUMED;
        });
    }
}