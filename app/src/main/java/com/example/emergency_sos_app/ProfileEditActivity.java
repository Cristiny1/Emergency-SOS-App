package com.example.emergency_sos_app;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

public class ProfileEditActivity extends BaseActivity {

    private String editType;
    private LinearLayout container;
    private SharedPreferences prefs;
    private final Map<String, String[]> provinceDistrictMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_edit);

        editType = getIntent().getStringExtra("EDIT_TYPE");
        container = findViewById(R.id.containerEditFields);
        prefs = getSharedPreferences("sos_profile_prefs", MODE_PRIVATE);

        initData();
        setupUI();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSave).setOnClickListener(v -> saveAndExit());
    }

    private void initData() {
        provinceDistrictMap.put("Siem Reap", new String[]{"Siem Reap City", "Prasat Bakong", "Banteay Srei", "Soutr Nikum"});
        provinceDistrictMap.put("Phnom Penh", new String[]{"Chamkar Mon", "Daun Penh", "7 Makara", "Tuol Kork"});
        provinceDistrictMap.put("Battambang", new String[]{"Battambang City", "Sangkae", "Banan", "Ek Phnom"});
        provinceDistrictMap.put("Sihanoukville", new String[]{"Preah Sihanouk", "Prey Nob", "Stueng Hav", "Kampong Seila"});
        provinceDistrictMap.put("Kampong Cham", new String[]{"Kampong Cham City", "Kampong Siem", "Kang Meas", "Koh Sotin"});
        provinceDistrictMap.put("Kandal", new String[]{"Ta Khmau City", "Kien Svay", "Lvea Aem", "Koh Thum"});
    }

    private void setupUI() {
        TextView title = findViewById(R.id.tvEditTitle);
        LayoutInflater inflater = LayoutInflater.from(this);

        if ("LOCATION".equals(editType)) {
            title.setText(R.string.location_information_label);
            View view = inflater.inflate(R.layout.layout_location_editor, container, true);
            
            Spinner spProvince = view.findViewById(R.id.spinnerProvince);
            Spinner spDistrict = view.findViewById(R.id.spinnerDistrict);
            
            setupSpinners(spProvince, spDistrict);
            
            ((EditText)view.findViewById(R.id.etCommune)).setText(prefs.getString("commune", ""));
            ((EditText)view.findViewById(R.id.etVillage)).setText(prefs.getString("village", ""));
            ((EditText)view.findViewById(R.id.etHomeAddress)).setText(prefs.getString("home_address", ""));
        } else if ("SAFETY".equals(editType)) {
            title.setText(R.string.section_safety_information);
            addSafetyFields();
        }
    }

    private void setupSpinners(Spinner spProvince, Spinner spDistrict) {
        ArrayAdapter<CharSequence> pAdapter = ArrayAdapter.createFromResource(this,
                R.array.provinces_cambodia, android.R.layout.simple_spinner_item);
        pAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spProvince.setAdapter(pAdapter);

        String savedProvince = prefs.getString("province", "Siem Reap");
        int pPos = pAdapter.getPosition(savedProvince);
        if (pPos >= 0) spProvince.setSelection(pPos);

        spProvince.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String province = parent.getItemAtPosition(position).toString();
                updateDistrictSpinner(spDistrict, province);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void updateDistrictSpinner(Spinner spDistrict, String province) {
        String[] districts = provinceDistrictMap.get(province);
        if (districts == null) districts = new String[]{"Select District"};
        
        ArrayAdapter<String> dAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, districts);
        dAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDistrict.setAdapter(dAdapter);

        String savedDistrict = prefs.getString("district", "");
        int dPos = dAdapter.getPosition(savedDistrict);
        if (dPos >= 0) spDistrict.setSelection(dPos);
    }

    private void addSafetyFields() {
        container.addView(createField(getString(R.string.blood_group_label), "blood_group", prefs.getString("blood_group", "O+")));
        container.addView(createField(getString(R.string.allergies_label), "allergies", prefs.getString("allergies", "None")));
        container.addView(createField(getString(R.string.conditions_label), "condition", prefs.getString("condition", "None")));
        container.addView(createField(getString(R.string.medical_notes), "medical_notes", prefs.getString("medical_notes", "")));
    }

    private View createField(String label, String key, String value) {
        View v = LayoutInflater.from(this).inflate(R.layout.item_profile_field_edit, container, false);
        ((TextView)v.findViewById(R.id.tvLabel)).setText(label);
        EditText et = v.findViewById(R.id.etValue);
        et.setText(value);
        et.setTag(key);
        return v;
    }

    private void saveAndExit() {
        playClickFeedback();
        SharedPreferences.Editor editor = prefs.edit();

        if ("LOCATION".equals(editType)) {
            Spinner spProvince = container.findViewById(R.id.spinnerProvince);
            Spinner spDistrict = container.findViewById(R.id.spinnerDistrict);
            
            if (spProvince != null) editor.putString("province", spProvince.getSelectedItem().toString());
            if (spDistrict != null) editor.putString("district", spDistrict.getSelectedItem().toString());
            
            EditText etCommune = container.findViewById(R.id.etCommune);
            EditText etVillage = container.findViewById(R.id.etVillage);
            EditText etHome = container.findViewById(R.id.etHomeAddress);
            
            if (etCommune != null) editor.putString("commune", etCommune.getText().toString());
            if (etVillage != null) editor.putString("village", etVillage.getText().toString());
            if (etHome != null) editor.putString("home_address", etHome.getText().toString());
        } else {
            for (int i = 0; i < container.getChildCount(); i++) {
                View v = container.getChildAt(i);
                EditText et = v.findViewById(R.id.etValue);
                if (et != null && et.getTag() != null) {
                    editor.putString(et.getTag().toString(), et.getText().toString());
                }
            }
        }

        editor.apply();
        Toast.makeText(this, R.string.profile_updated_success, Toast.LENGTH_SHORT).show();
        finish();
    }
}
