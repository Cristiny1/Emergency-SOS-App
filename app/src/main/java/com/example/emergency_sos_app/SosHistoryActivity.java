package com.example.emergency_sos_app;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SosHistoryActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sos_history);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        setupNavigation(0); // Not in bottom nav

        loadHistory();
    }

    private void loadHistory() {
        LinearLayout container = findViewById(R.id.historyContainer);
        if (container == null) return;
        container.removeAllViews();

        List<HistoryManager.HistoryEvent> events = HistoryManager.getHistory(this);
        if (events.isEmpty()) {
            View emptyView = LayoutInflater.from(this).inflate(R.layout.item_history_empty, container, false);
            container.addView(emptyView);
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy • hh:mm a", Locale.getDefault());
        LayoutInflater inflater = LayoutInflater.from(this);

        for (HistoryManager.HistoryEvent event : events) {
            View item = inflater.inflate(R.layout.item_history_event, container, false);
            
            ImageView ivIcon = item.findViewById(R.id.ivHistoryIcon);
            TextView tvType = item.findViewById(R.id.tvHistoryType);
            TextView tvLoc = item.findViewById(R.id.tvHistoryLocation);
            TextView tvTime = item.findViewById(R.id.tvHistoryTime);
            View btnDelete = item.findViewById(R.id.btnDeleteEvent);

            tvType.setText(event.type + " SOS");
            tvLoc.setText(event.location);
            tvTime.setText(sdf.format(new Date(event.timestamp)));

            // Color and Icon based on type
            if ("Medical".equalsIgnoreCase(event.type)) {
                tvType.setTextColor(getColor(R.color.m_red));
                ivIcon.setImageResource(R.drawable.ic_medical);
                ivIcon.setBackgroundTintList(ColorStateList.valueOf(0x33C22026)); // 20% m_red
            } else if ("Police".equalsIgnoreCase(event.type)) {
                tvType.setTextColor(getColor(R.color.fb_blue));
                ivIcon.setImageResource(R.drawable.ic_police_badge);
                ivIcon.setBackgroundTintList(ColorStateList.valueOf(0x331877F2)); // 20% fb_blue
            } else if ("Fire".equalsIgnoreCase(event.type)) {
                tvType.setTextColor(getColor(R.color.m_orange));
                ivIcon.setImageResource(R.drawable.fire);
                ivIcon.setBackgroundTintList(ColorStateList.valueOf(0x33F7941E)); // 20% m_orange
            }

            btnDelete.setOnClickListener(v -> {
                HistoryManager.deleteEvent(this, event.timestamp);
                loadHistory();
                Toast.makeText(this, "Event deleted", Toast.LENGTH_SHORT).show();
            });

            container.addView(item);
        }
    }

    private void setupEdgeToEdge() {
        View toolbar = findViewById(R.id.toolbar);
        View bottomNav = findViewById(R.id.bottomNavigation);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            int bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;

            if (toolbar != null) {
                toolbar.setPadding(toolbar.getPaddingLeft(), top, toolbar.getPaddingRight(), toolbar.getPaddingBottom());
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
