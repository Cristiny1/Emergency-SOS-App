package com.example.emergency_sos_app;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class NavigationHelper {

    private static final String TAG = "NavigationHelper";

    public static void setup(Activity activity, int currentId) {
        BottomNavigationView nav = activity.findViewById(R.id.bottomNavigation);
        if (nav == null) return;

        // Set the current item without triggering the listener
        nav.setOnItemSelectedListener(null);
        nav.setSelectedItemId(currentId);

        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            Log.d(TAG, "Selected: " + id + " (Current: " + currentId + ")");

            // Haptic Feedback for premium feel
            if (activity instanceof BaseActivity) {
                ((BaseActivity) activity).playClickFeedback();
            }

            if (id == currentId) {
                // If user clicks the current tab, we usually do nothing or scroll to top.
                // We avoid calling smoothRefresh() here because for singleTop activities 
                // like Dashboard, it can cause the app to close.
                return true;
            }

            Class<?> target = null;
            if (id == R.id.nav_home) target = DashboardActivity.class;
            else if (id == R.id.nav_news) target = NewsActivity.class;
            else if (id == R.id.nav_profile) target = ProfileActivity.class;
            else if (id == R.id.nav_notifications) target = AlertsActivity.class;
            else if (id == R.id.nav_family) target = FamilyActivity.class;

            if (target != null) {
                Intent intent = new Intent(activity, target);
                // Use a more standard flag for home/tab navigation
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                activity.startActivity(intent);
                // Instant switch to avoid flicker
                activity.overridePendingTransition(0, 0);
                return true;
            }

            return false;
        });
    }
}