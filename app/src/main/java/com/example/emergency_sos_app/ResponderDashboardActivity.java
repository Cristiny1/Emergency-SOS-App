
package com.example.emergency_sos_app;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.emergency_sos_app.network.RetrofitClient;

public class ResponderDashboardActivity extends BaseActivity {

    private TextView tvConnection;
    private View connectionDot;

    private TextView tvActiveIncidents;
    private TextView tvAssignedIncidents;
    private TextView tvResolvedIncidents;

    private TextView tvIncidentsEmpty;
    private TextView tvResponderError;
    private ProgressBar progressIncidents;
    private RecyclerView rvResponderIncidents;

    private Button btnCheckConnection;
    private Button btnRefreshIncidents;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!RoleManager.hasResponderAccess(this)) {
            Toast.makeText(
                    this,
                    R.string.responder_access_denied,
                    Toast.LENGTH_LONG
            ).show();

            startFadeActivity(
                    new Intent(this, RoleSelectionActivity.class)
            );

            finish();
            return;
        }

        setContentView(R.layout.activity_responder_dashboard);

        bindViews();
        setupListeners();
        updateConnectionStatus();

        // Load incident data after connecting your API method.
    }

    private void bindViews() {
        tvConnection = findViewById(R.id.tvResponderConnection);
        connectionDot = findViewById(R.id.responderConnectionDot);

        tvActiveIncidents = findViewById(R.id.tvActiveIncidents);
        tvAssignedIncidents = findViewById(R.id.tvAssignedIncidents);
        tvResolvedIncidents = findViewById(R.id.tvResolvedIncidents);

        tvIncidentsEmpty = findViewById(R.id.tvIncidentsEmpty);
        tvResponderError = findViewById(R.id.tvResponderError);
        progressIncidents = findViewById(R.id.progressIncidents);
        rvResponderIncidents = findViewById(R.id.rvResponderIncidents);

        btnCheckConnection =
                findViewById(R.id.btnResponderCheckConnection);

        btnRefreshIncidents =
                findViewById(R.id.btnRefreshIncidents);

        rvResponderIncidents.setNestedScrollingEnabled(false);
    }

    private void setupListeners() {
        btnCheckConnection.setOnClickListener(v -> {
            tvConnection.setText(
                    R.string.responder_connection_checking
            );

            btnCheckConnection.setEnabled(false);

            String token = RetrofitClient.getAccessToken();

            if (token == null || token.trim().isEmpty()) {
                updateConnectionStatus();

                btnCheckConnection.setEnabled(true);

                Toast.makeText(
                        this,
                        "Please sign in before connecting.",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            SocketManager.getInstance().connect(token);

            // This is a temporary UI refresh, not proof
            // that a connection has succeeded.
            tvConnection.postDelayed(() -> {
                updateConnectionStatus();
                btnCheckConnection.setEnabled(true);
            }, 1500);
        });

        btnRefreshIncidents.setOnClickListener(v -> {
            loadIncidents();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateConnectionStatus();
    }

    private void updateConnectionStatus() {
        if (tvConnection == null || connectionDot == null) {
            return;
        }

        boolean connected =
                SocketManager.getInstance().isConnected();

        tvConnection.setText(
                connected
                        ? R.string.responder_connection_online
                        : R.string.responder_connection_offline
        );

        int color = ContextCompat.getColor(
                this,
                connected
                        ? R.color.green_verified
                        : R.color.text_gray
        );

        connectionDot.setBackgroundTintList(
                ColorStateList.valueOf(color)
        );
    }

    private void loadIncidents() {
        /*
         * TODO:
         * 1. Call the real incidents endpoint using Retrofit.
         * 2. Parse the API response.
         * 3. Update the RecyclerView adapter.
         * 4. Calculate Active, Assigned, and Resolved totals.
         */

        progressIncidents.setVisibility(View.VISIBLE);
        tvResponderError.setVisibility(View.GONE);

        btnRefreshIncidents.setEnabled(false);

        // Remove this placeholder when Retrofit is implemented.
        progressIncidents.setVisibility(View.GONE);
        btnRefreshIncidents.setEnabled(true);

        Toast.makeText(
                this,
                "Connect your incidents API to load SOS alerts.",
                Toast.LENGTH_SHORT
        ).show();
    }
}
