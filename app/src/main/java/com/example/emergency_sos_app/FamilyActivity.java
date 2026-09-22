package com.example.emergency_sos_app;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;
import java.util.UUID;

public class FamilyActivity extends BaseActivity {

    private LinearLayout container;
    private String[] relationshipOptions;

    private final ActivityResultLauncher<Intent> contactPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    handleContactResult(result.getData().getData());
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_family);

        container = findViewById(R.id.familyMembersContainer);
        relationshipOptions = getResources().getStringArray(R.array.relationship_options);

        setupNavigation(R.id.nav_family);

        findViewById(R.id.btnCheckIn).setOnClickListener(v -> 
                Toast.makeText(this, R.string.location_shared_family, Toast.LENGTH_LONG).show());

        findViewById(R.id.btnAddMember).setOnClickListener(v -> showMemberDialog(null));
        findViewById(R.id.btnPickContact).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
            contactPickerLauncher.launch(intent);
        });
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        loadFamilyMembers();
    }

    private void handleContactResult(Uri contactUri) {
        String[] projection = {
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
        };

        try (Cursor cursor = getContentResolver().query(contactUri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                String name = cursor.getString(0);
                String phone = cursor.getString(1);
                
                // Show dialog with pre-filled data
                FamilyManager.FamilyMember draft = new FamilyManager.FamilyMember(
                        UUID.randomUUID().toString(), name, phone, relationshipOptions[relationshipOptions.length - 1]);
                showMemberDialog(draft);
            }
        } catch (Exception e) {
            Toast.makeText(this, R.string.failed_read_contact, Toast.LENGTH_SHORT).show();
        }
    }

    private void loadFamilyMembers() {
        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        List<FamilyManager.FamilyMember> members = FamilyManager.getMembers(this);
        
        View emptyState = findViewById(R.id.layoutFamilyEmpty);
        if (members.isEmpty()) {
            if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
            return;
        }
        if (emptyState != null) emptyState.setVisibility(View.GONE);

        for (FamilyManager.FamilyMember member : members) {
            View view = inflater.inflate(R.layout.item_family_member, container, false);
            
            TextView tvName = view.findViewById(R.id.tvMemberName);
            TextView tvPhone = view.findViewById(R.id.tvMemberPhone);
            TextView tvRel = view.findViewById(R.id.tvMemberRelationship);
            
            ImageView btnCall = view.findViewById(R.id.btnCall);
            ImageView btnMessage = view.findViewById(R.id.btnMessage);
            ImageView btnEdit = view.findViewById(R.id.btnEdit);
            ImageView btnDelete = view.findViewById(R.id.btnDelete);

            tvName.setText(member.name);
            tvPhone.setText(member.phone);
            tvRel.setText(member.relationship.toUpperCase());

            btnCall.setOnClickListener(v -> {
                startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + member.phone)));
            });

            btnMessage.setOnClickListener(v -> {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("sms:" + member.phone)));
            });

            btnEdit.setOnClickListener(v -> showMemberDialog(member));
            btnDelete.setOnClickListener(v -> confirmDelete(member));

            container.addView(view);
        }
    }

    private void showMemberDialog(FamilyManager.FamilyMember existingMember) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 40, 60, 10);

        EditText etName = new EditText(this);
        etName.setHint(R.string.contact_name_hint);
        etName.setBackgroundResource(R.drawable.bg_edittext);
        etName.setPadding(40, 40, 40, 40);
        etName.setText(existingMember != null ? existingMember.name : "");
        layout.addView(etName);

        EditText etPhone = new EditText(this);
        etPhone.setHint(R.string.phone_number_hint);
        etPhone.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        etPhone.setBackgroundResource(R.drawable.bg_edittext);
        etPhone.setPadding(40, 40, 40, 40);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = 30;
        etPhone.setLayoutParams(lp);
        etPhone.setText(existingMember != null ? existingMember.phone : "");
        layout.addView(etPhone);

        TextView labelRel = new TextView(this);
        labelRel.setText(R.string.relationship_label);
        labelRel.setPadding(10, 30, 0, 10);
        layout.addView(labelRel);

        Spinner spinnerRel = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, relationshipOptions);
        spinnerRel.setAdapter(adapter);
        
        if (existingMember != null) {
            for (int i = 0; i < relationshipOptions.length; i++) {
                if (relationshipOptions[i].equalsIgnoreCase(existingMember.relationship)) {
                    spinnerRel.setSelection(i);
                    break;
                }
            }
        }
        layout.addView(spinnerRel);

        new MaterialAlertDialogBuilder(this)
                .setTitle(existingMember == null ? R.string.add_family_member_title : R.string.edit_family_member_title)
                .setView(layout)
                .setPositiveButton(R.string.save_label, (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String phone = etPhone.getText().toString().trim();
                    String rel = relationshipOptions[spinnerRel.getSelectedItemPosition()];
                    
                    if (name.isEmpty() || phone.isEmpty()) {
                        Toast.makeText(this, R.string.fill_all_fields, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String id = (existingMember != null && existingMember.id != null) ? existingMember.id : UUID.randomUUID().toString();
                    FamilyManager.saveMember(this, new FamilyManager.FamilyMember(id, name, phone, rel));
                    loadFamilyMembers();
                    Toast.makeText(this, R.string.family_circle_updated, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void confirmDelete(FamilyManager.FamilyMember member) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.remove_member_title)
                .setMessage(getString(R.string.remove_member_confirm, member.name))
                .setPositiveButton(R.string.remove_label, (dialog, which) -> {
                    FamilyManager.deleteMember(this, member.id);
                    loadFamilyMembers();
                    Toast.makeText(this, R.string.member_removed, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void setupEdgeToEdge() {
        View toolbar = findViewById(R.id.toolbar);
        View bottomNav = findViewById(R.id.bottomNavigation);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            int bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;

            if (toolbar != null) {
                toolbar.setPadding(0, top, 0, 0);
            }
            if (bottomNav != null) {
                RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) bottomNav.getLayoutParams();
                lp.bottomMargin = (int) (16 * getResources().getDisplayMetrics().density) + bottom;
                bottomNav.setLayoutParams(lp);
            }
            return WindowInsetsCompat.CONSUMED;
        });
    }
}
