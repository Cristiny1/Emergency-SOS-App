package com.example.emergency_sos_app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import de.hdodenhof.circleimageview.CircleImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class ProfileActivity extends BaseActivity {

    private static final String PREFS_NAME = "sos_profile_prefs";
    private CircleImageView ivProfile;
    private TextView tvSummaryPersonal, tvSummaryLocation, tvSummaryFamily, tvSummarySafety;
    private SharedPreferences prefs;
    private ActivityResultLauncher<String> photoPickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        ivProfile = findViewById(R.id.ivProfile);
        tvSummaryPersonal = findViewById(R.id.tvSummaryPersonal);
        tvSummaryLocation = findViewById(R.id.tvSummaryLocation);
        tvSummaryFamily = findViewById(R.id.tvSummaryFamily);
        tvSummarySafety = findViewById(R.id.tvSummarySafety);

        setupPhotoPicker();
        loadProfileData();
        setupNavigation(R.id.nav_profile);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnEditPhoto).setOnClickListener(v -> {
            playClickFeedback();
            photoPickerLauncher.launch("image/*");
        });

        // Menu Row Clicks
        findViewById(R.id.rowPersonalInfo).setOnClickListener(v -> {
            playClickFeedback();
            Intent intent = new Intent(this, AccountDetailActivity.class);
            androidx.core.app.ActivityOptionsCompat options = androidx.core.app.ActivityOptionsCompat.makeSceneTransitionAnimation(this, ivProfile, "profile_photo");
            startActivity(intent, options.toBundle());
        });

        findViewById(R.id.rowLocationInfo).setOnClickListener(v -> {
            playClickFeedback();
            Intent intent = new Intent(this, ProfileEditActivity.class);
            intent.putExtra("EDIT_TYPE", "LOCATION");
            startFadeActivity(intent);
        });

        findViewById(R.id.rowFamilyCircle).setOnClickListener(v -> {
            playClickFeedback();
            startFadeActivity(new Intent(this, FamilyActivity.class));
        });

        findViewById(R.id.rowSafetyInfo).setOnClickListener(v -> {
            playClickFeedback();
            Intent intent = new Intent(this, ProfileEditActivity.class);
            intent.putExtra("EDIT_TYPE", "SAFETY");
            startFadeActivity(intent);
        });

        findViewById(R.id.rowAccountSecurity).setOnClickListener(v -> {
            playClickFeedback();
            startFadeActivity(new Intent(this, AccountDetailActivity.class));
        });

        findViewById(R.id.rowLanguage).setOnClickListener(v -> {
            playClickFeedback();
            LanguagePickerDialog.show(this);
        });

        findViewById(R.id.rowPrivacySettings).setOnClickListener(v -> {
            playClickFeedback();
            Intent intent = new Intent(this, SettingsDetailActivity.class);
            intent.putExtra("TYPE", "privacy");
            startFadeActivity(intent);
        });
    }

    private void setupPhotoPicker() {
        photoPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri == null) return;
                    File savedFile = copyImageToInternalStorage(uri);
                    if (savedFile != null) {
                        ivProfile.setImageURI(Uri.fromFile(savedFile));
                        prefs.edit().putString("profile_photo_path", savedFile.getAbsolutePath()).apply();
                        vibrate(40);
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

    private void loadProfileData() {
        String photoPath = prefs.getString("profile_photo_path", null);
        if (photoPath != null) {
            File photoFile = new File(photoPath);
            if (photoFile.exists()) ivProfile.setImageURI(Uri.fromFile(photoFile));
        } else {
            ivProfile.setImageResource(R.drawable.ic_personal_white);
        }

        // Update Summaries with safety checks
        String name = prefs.getString("name", getString(R.string.default_user_name));
        String gender = prefs.getString("gender", getString(R.string.unspecified));
        if (tvSummaryPersonal != null) tvSummaryPersonal.setText(name + ", " + gender);

        String province = prefs.getString("province", getString(R.string.default_province));
        String village = prefs.getString("village", getString(R.string.default_country));
        if (tvSummaryLocation != null) tvSummaryLocation.setText(province + ", " + village);

        String blood = prefs.getString("blood_group", getString(R.string.default_blood_group));
        String allergies = prefs.getString("allergies", getString(R.string.no_allergies));
        if (tvSummarySafety != null) tvSummarySafety.setText(getString(R.string.blood_type_prefix, blood) + ", " + allergies);

        // Optional: Manage family members message
        if (tvSummaryFamily != null) {
            tvSummaryFamily.setText("Manage emergency contacts");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfileData(); // Refresh photo and info if changed
    }
}
