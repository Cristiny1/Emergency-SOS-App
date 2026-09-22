package com.example.emergency_sos_app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.emergency_sos_app.models.ApiResponse;
import com.example.emergency_sos_app.network.FileHelper;
import com.example.emergency_sos_app.network.RetrofitClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CitizenReportActivity extends BaseActivity {

    private ImageView ivPreview, ivVideoPreview;
    private LinearLayout layoutUploadHint, layoutVideoHint;
    private TextView tvReportAddress, tvCriticalWarning, tvSubmittingState;
    private ProgressBar progressSubmitting;
    private WebView mapWebView;
    private ChipGroup chipGroupSeverity;
    
    private boolean isMapLoaded = false;
    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<String> photoPickerLauncher;
    private ActivityResultLauncher<Uri> videoCaptureLauncher;

    private double currentLat = 13.3633;
    private double currentLng = 103.8564;
    private Uri selectedPhotoUri, capturedVideoUri;

    // Voice Recording
    private MediaRecorder mediaRecorder;
    private String audioPath;
    private boolean isRecording = false;
    private ImageView ivVoiceIcon;
    private TextView tvVoiceLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_citizen_report);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        
        initViews();
        setupWebView();
        setupSeverityLogic();
        setupMediaLaunchers();

        findViewById(R.id.btnUploadPhoto).setOnClickListener(v -> photoPickerLauncher.launch("image/*"));
        findViewById(R.id.btnUploadVideo).setOnClickListener(v -> attemptVideoCapture());
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnVoiceRecord).setOnClickListener(v -> toggleRecording());

        findViewById(R.id.btnSubmitReport).setOnClickListener(v -> attemptSubmit());

        requestLocation();
    }

    private void initViews() {
        ivPreview = findViewById(R.id.ivPreview);
        ivVideoPreview = findViewById(R.id.ivVideoPreview);
        layoutUploadHint = findViewById(R.id.layoutUploadHint);
        layoutVideoHint = findViewById(R.id.layoutVideoHint);
        tvReportAddress = findViewById(R.id.tvReportAddress);
        tvCriticalWarning = findViewById(R.id.tvCriticalWarning);
        tvSubmittingState = findViewById(R.id.tvSubmittingState);
        progressSubmitting = findViewById(R.id.progressSubmitting);
        mapWebView = findViewById(R.id.mapWebView);
        chipGroupSeverity = findViewById(R.id.chipGroupSeverity);
        ivVoiceIcon = findViewById(R.id.ivVoiceIcon);
        tvVoiceLabel = findViewById(R.id.tvVoiceLabel);
    }

    private void setupMediaLaunchers() {
        photoPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedPhotoUri = uri;
                        ivPreview.setImageURI(uri);
                        layoutUploadHint.setVisibility(View.GONE);
                    }
                });

        videoCaptureLauncher = registerForActivityResult(
                new ActivityResultContracts.CaptureVideo(),
                success -> {
                    if (success) {
                        layoutVideoHint.setVisibility(View.GONE);
                        ivVideoPreview.setVisibility(View.VISIBLE);
                        ivVideoPreview.setImageResource(R.drawable.hp); // Placeholder icon for video present
                        Toast.makeText(this, R.string.video_captured_success, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void attemptVideoCapture() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 100);
            return;
        }

        File videoFile = new File(getExternalCacheDir(), "safety_video.mp4");
        capturedVideoUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", videoFile);
        videoCaptureLauncher.launch(capturedVideoUri);
    }

    private void attemptSubmit() {
        EditText etDesc = findViewById(R.id.etDescription);
        String desc = etDesc.getText().toString().trim();
        if (desc.isEmpty()) {
            etDesc.setError(getString(R.string.report_description_required));
            return;
        }

        setSubmittingState(true);

        String category = ((Spinner)findViewById(R.id.spinnerIssueCategory)).getSelectedItem().toString();
        String severity = getSelectedSeverity();
        
        // Prepare Multipart Parts
        RequestBody categoryBody = RequestBody.create(category, MediaType.parse("text/plain"));
        RequestBody severityBody = RequestBody.create(severity, MediaType.parse("text/plain"));
        RequestBody descBody = RequestBody.create(desc, MediaType.parse("text/plain"));
        RequestBody latBody = RequestBody.create(String.valueOf(currentLat), MediaType.parse("text/plain"));
        RequestBody lngBody = RequestBody.create(String.valueOf(currentLng), MediaType.parse("text/plain"));

        MultipartBody.Part photoPart = null;
        if (selectedPhotoUri != null) {
            File file = FileHelper.getFile(this, selectedPhotoUri);
            if (file != null) {
                RequestBody requestFile = RequestBody.create(file, MediaType.parse("image/*"));
                photoPart = MultipartBody.Part.createFormData("photo", file.getName(), requestFile);
            }
        }

        MultipartBody.Part voicePart = null;
        if (audioPath != null) {
            File file = new File(audioPath);
            if (file.exists()) {
                RequestBody requestFile = RequestBody.create(file, MediaType.parse("audio/*"));
                voicePart = MultipartBody.Part.createFormData("voice", file.getName(), requestFile);
            }
        }

        RetrofitClient.getApiService().createReport(categoryBody, severityBody, descBody, latBody, lngBody, photoPart, voicePart)
                .enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                setSubmittingState(false);
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    Toast.makeText(CitizenReportActivity.this, R.string.report_submitted_center, Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(CitizenReportActivity.this, R.string.submission_failed, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                setSubmittingState(false);
                Toast.makeText(CitizenReportActivity.this, R.string.network_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void toggleRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 200);
            return;
        }

        if (!isRecording) {
            startRecording();
        } else {
            stopRecording();
        }
    }

    private void startRecording() {
        audioPath = getExternalCacheDir().getAbsolutePath() + "/voice_report.3gp";
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mediaRecorder.setOutputFile(audioPath);

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            ivVoiceIcon.setColorFilter(getColor(R.color.sos_red));
            tvVoiceLabel.setText("Recording...");
        } catch (IOException e) {
            Log.e("VoiceRecord", "prepare() failed");
        }
    }

    private void stopRecording() {
        if (mediaRecorder != null) {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            isRecording = false;
            ivVoiceIcon.setColorFilter(getColor(R.color.green_verified));
            tvVoiceLabel.setText("Recorded");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == 200) toggleRecording();
            if (requestCode == 100) attemptVideoCapture();
        }
    }

    private void setupSeverityLogic() {
        chipGroupSeverity.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipCritical) {
                tvCriticalWarning.setVisibility(View.VISIBLE);
            } else {
                tvCriticalWarning.setVisibility(View.GONE);
            }
        });
    }

    private String getSelectedSeverity() {
        int id = chipGroupSeverity.getCheckedChipId();
        if (id == R.id.chipCritical) return "CRITICAL";
        if (id == R.id.chipHigh) return "HIGH";
        if (id == R.id.chipMedium) return "MEDIUM";
        return "LOW";
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings settings = mapWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        mapWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                isMapLoaded = true;
                updateWebViewLocation();
            }
        });

        mapWebView.loadUrl("file:///android_asset/leaflet_map.html");
    }

    private void requestLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    currentLat = location.getLatitude();
                    currentLng = location.getLongitude();
                    updateWebViewLocation();
                    updateAddressLabel(currentLat, currentLng);
                } else {
                    updateAddressLabel(currentLat, currentLng);
                }
            });
        } else {
            updateAddressLabel(currentLat, currentLng);
        }
    }

    private void updateWebViewLocation() {
        if (!isMapLoaded) return;
        mapWebView.evaluateJavascript("setLocation(" + currentLat + "," + currentLng + ", 'Report Location')", null);
    }

    private void setSubmittingState(boolean submitting) {
        Button btnSubmit = findViewById(R.id.btnSubmitReport);
        if (btnSubmit != null) {
            btnSubmit.setEnabled(!submitting);
            btnSubmit.setText(submitting ? getString(R.string.report_submitting) : getString(R.string.report_submit_button));
        }
        if (progressSubmitting != null) {
            progressSubmitting.setVisibility(submitting ? View.VISIBLE : View.GONE);
        }
        if (tvSubmittingState != null) {
            tvSubmittingState.setVisibility(submitting ? View.VISIBLE : View.GONE);
            tvSubmittingState.setText(submitting ? getString(R.string.report_processing_message) : "");
        }
    }

    private void updateAddressLabel(double lat, double lng) {
        new Thread(() -> {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            String resolvedAddress = null;
            try {
                List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address addr = addresses.get(0);
                    resolvedAddress = addr.getAddressLine(0);
                    if (resolvedAddress != null && !resolvedAddress.toLowerCase().contains("cambodia")) {
                        resolvedAddress += ", Cambodia";
                    }
                }
            } catch (Exception e) {
                resolvedAddress = String.format(Locale.getDefault(), "Lat: %.4f, Lng: %.4f", lat, lng);
            }

            final String finalAddr = resolvedAddress;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed() || tvReportAddress == null) return;
                tvReportAddress.setText(finalAddr != null ? finalAddr : getString(R.string.report_detecting_location));
            });
        }).start();
    }
}
