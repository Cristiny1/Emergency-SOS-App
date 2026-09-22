package com.example.emergency_sos_app;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Administrative Console for SOS Center.
 * Currently localized for the 100% Khmer sweep.
 */
public class SosCenterActivity extends BaseActivity {

    private TextView tvLogs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sos_center);

        tvLogs = findViewById(R.id.tvLogs);
        
        Button btnAssign = findViewById(R.id.btnAssign);
        Button btnResolve = findViewById(R.id.btnResolve);
        Button btnClose = findViewById(R.id.btnClose);

        btnAssign.setOnClickListener(v -> {
            playClickFeedback();
            Toast.makeText(this, R.string.status_updated_assigned, Toast.LENGTH_SHORT).show();
        });

        btnResolve.setOnClickListener(v -> {
            playClickFeedback();
            Toast.makeText(this, R.string.status_updated_completed, Toast.LENGTH_SHORT).show();
        });

        btnClose.setOnClickListener(v -> finish());
    }
}
