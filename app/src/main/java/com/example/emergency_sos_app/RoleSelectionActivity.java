package com.example.emergency_sos_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;

public class RoleSelectionActivity extends BaseActivity {

    private String selectedRole = RoleManager.ROLE_USER;

    private MaterialCardView roleUserCard;
    private MaterialCardView roleAdminCard;
    private MaterialCardView roleResponderCard;

    private TextView tvRoleUserTitle;
    private TextView tvRoleUserDescription;
    private TextView tvRoleUserCheck;

    private Button btnContinueRole;
    private View btnBackRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_selection);

        initializeViews();
        setupRoleCards();
        setupButtons();
        updateSelection();
    }

    private void initializeViews() {
        roleUserCard = findViewById(R.id.roleCardUser);
        roleAdminCard = findViewById(R.id.roleCardAdmin);
        roleResponderCard = findViewById(R.id.roleCardResponder);

        tvRoleUserTitle = findViewById(R.id.tvRoleUserTitle);
        tvRoleUserDescription =
                findViewById(R.id.tvRoleUserDescription);
        tvRoleUserCheck = findViewById(R.id.tvRoleUserCheck);

        btnContinueRole = findViewById(R.id.btnContinueRole);
        btnBackRole = findViewById(R.id.btnBackRole);
    }

    private void setupRoleCards() {

        // Citizen: អាចចុះឈ្មោះបាន
        roleUserCard.setOnClickListener(v -> {
            selectedRole = RoleManager.ROLE_USER;
            updateSelection();
        });

        // Admin: ត្រូវការការអនុញ្ញាត
        roleAdminCard.setOnClickListener(v -> {
            showRestrictedMessage("Admin");
        });

        // Emergency Responder: ត្រូវការការផ្ទៀងផ្ទាត់
        roleResponderCard.setOnClickListener(v -> {
            showRestrictedMessage("Emergency Responder");
        });
    }

    private void setupButtons() {

        btnBackRole.setOnClickListener(v -> finish());

        btnContinueRole.setOnClickListener(v -> {

            // អនុញ្ញាតតែ Citizen ប៉ុណ្ណោះ
            if (!isSelectableRole(selectedRole)) {
                selectedRole = RoleManager.ROLE_USER;
                updateSelection();
            }

            RoleManager.setSelectedRole(this, selectedRole);

            // ទៅកាន់ទំព័រចុះឈ្មោះ Citizen
            Intent intent = new Intent(
                    RoleSelectionActivity.this,
                    SignupActivity.class
            );

            intent.putExtra("USER_ROLE", RoleManager.ROLE_USER);
            intent.putExtra("SELECTED_ROLE", RoleManager.ROLE_USER);

            startFadeActivity(intent);
        });
    }

    private boolean isSelectableRole(String role) {
        return RoleManager.ROLE_USER.equals(role);
    }

    private void showRestrictedMessage(String role) {
        Toast.makeText(
                this,
                role + " access requires authorization.",
                Toast.LENGTH_LONG
        ).show();
    }

    private void updateSelection() {

        boolean citizenSelected =
                isSelectableRole(selectedRole);

        int primary = ContextCompat.getColor(
                this, R.color.primary
        );

        int border = ContextCompat.getColor(
                this, R.color.border_gray
        );

        int cardBackground = ContextCompat.getColor(
                this, R.color.card_background
        );

        int textDark = ContextCompat.getColor(
                this, R.color.text_dark
        );

        int textGray = ContextCompat.getColor(
                this, R.color.text_gray
        );

        // Citizen card
        roleUserCard.setCardBackgroundColor(cardBackground);
        roleUserCard.setStrokeColor(
                citizenSelected ? primary : border
        );
        roleUserCard.setStrokeWidth(
                dpToPx(citizenSelected ? 2 : 1)
        );

        tvRoleUserTitle.setTextColor(textDark);
        tvRoleUserDescription.setTextColor(textGray);

        // Check mark
        tvRoleUserCheck.setVisibility(
                citizenSelected ? View.VISIBLE : View.INVISIBLE
        );

        // Admin and Responder remain restricted
        roleAdminCard.setCardBackgroundColor(cardBackground);
        roleAdminCard.setStrokeColor(border);
        roleAdminCard.setStrokeWidth(dpToPx(1));

        roleResponderCard.setCardBackgroundColor(cardBackground);
        roleResponderCard.setStrokeColor(border);
        roleResponderCard.setStrokeWidth(dpToPx(1));

        btnContinueRole.setText(
                R.string.role_continue_as_citizen
        );
    }

    private int dpToPx(int dp) {
        return Math.round(
                dp * getResources()
                        .getDisplayMetrics().density
        );
    }
}
