package com.example.emergency_sos_app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CalendarActivity extends BaseActivity {

    private CalendarView calendarView;
    private LinearLayout eventContainer;
    private TextView tvSelectedDate;
    private String selectedDateStr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        setupEdgeToEdge();
        initViews();
        
        // Default to today
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        selectedDateStr = sdf.format(new Date());
        updateDateLabel(new Date());
        loadEvents();
    }

    private void initViews() {
        calendarView = findViewById(R.id.calendarView);
        eventContainer = findViewById(R.id.eventContainer);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnAddEvent).setOnClickListener(v -> showAddEventDialog());

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, dayOfMonth);
            Date date = cal.getTime();
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            selectedDateStr = sdf.format(date);
            
            updateDateLabel(date);
            loadEvents();
        });
    }

    private void updateDateLabel(Date date) {
        SimpleDateFormat displaySdf = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
        tvSelectedDate.setText("Reminders for " + displaySdf.format(date));
    }

    private void loadEvents() {
        eventContainer.removeAllViews();
        List<String> events = CalendarManager.getEvents(this, selectedDateStr);
        
        if (events.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.no_reminders_date);
            empty.setTextColor(android.graphics.Color.BLACK);
            empty.setPadding(0, 40, 0, 0);
            empty.setGravity(android.view.Gravity.CENTER);
            eventContainer.addView(empty);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < events.size(); i++) {
            final int index = i;
            String desc = events.get(i);
            View item = inflater.inflate(R.layout.item_calendar_event, eventContainer, false);
            
            TextView tvDesc = item.findViewById(R.id.tvEventDescription);
            View btnDelete = item.findViewById(R.id.btnDeleteEvent);

            tvDesc.setText(desc);
            btnDelete.setOnClickListener(v -> {
                CalendarManager.deleteEvent(this, selectedDateStr, index);
                loadEvents();
                Toast.makeText(this, "Reminder removed", Toast.LENGTH_SHORT).show();
            });

            eventContainer.addView(item);
        }
    }

    private void showAddEventDialog() {
        EditText input = new EditText(this);
        input.setHint("Reminder description");
        
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 40, 60, 10);
        layout.addView(input);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Add Reminder")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    String desc = input.getText().toString().trim();
                    if (!desc.isEmpty()) {
                        CalendarManager.addEvent(this, selectedDateStr, desc);
                        loadEvents();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupEdgeToEdge() {
        View header = findViewById(R.id.calendarHeader);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            if (header != null) {
                header.setPadding(header.getPaddingLeft(), top, header.getPaddingRight(), header.getPaddingBottom());
            }
            return WindowInsetsCompat.CONSUMED;
        });
    }
}
