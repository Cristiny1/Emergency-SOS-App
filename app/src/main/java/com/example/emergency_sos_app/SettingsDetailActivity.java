package com.example.emergency_sos_app;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

public class SettingsDetailActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_detail);

        ImageView btnBack = findViewById(R.id.btnBack);
        TextView tvTitle = findViewById(R.id.tvTitle);
        TextView tvContent = findViewById(R.id.tvContent);

        String type = getIntent().getStringExtra("TYPE");
        if ("privacy".equals(type)) {
            tvTitle.setText(R.string.privacy_security);
            tvContent.setText("At Emergency SOS, your privacy is our highest priority. Our systems are designed for maximum safety with minimal data exposure.\n\n" +
                    "• End-to-End Encryption: Your location broadcasts are encrypted and visible only to authorized responders.\n\n" +
                    "• Zero-Retention Policy: We do not store your location history once an emergency case is closed.\n\n" +
                    "• GDPR Compliant: We follow international standards for data protection.\n\n" +
                    "• Permission Transparency: Every permission requested (GPS, Camera, Contacts) is strictly used for life-saving features only.\n\n" +
                    "For detailed inquiries, reach out to our DPO at privacy@sos-cambodia.gov.kh");
        } else if ("help".equals(type)) {
            tvTitle.setText(R.string.help_support);
            tvContent.setText("Welcome to the Emergency SOS Support Center. Here is how to maximize your safety:\n\n" +
                    "1. The Golden Rule: In a real emergency, hold the RED SOS button for 2 full seconds. Do not release until you feel the vibration.\n\n" +
                    "2. Family Circle: Ensure you have added at least 2 family members. They will receive SMS and push notifications with your GPS link.\n\n" +
                    "3. Dashboard Map: The background map shows your real-time position. If you see 'Location Locked', please check your GPS settings.\n\n" +
                    "4. Contacting Authorities: Use the Action Hub to directly dial 117 (Police) or 118 (Fire/Ambulance) without leaving the app.\n\n" +
                    "5. Offline Mode: If internet is lost, the app will attempt to broadcast via encrypted SMS automatically.");
        }

        btnBack.setOnClickListener(v -> finish());
    }
}