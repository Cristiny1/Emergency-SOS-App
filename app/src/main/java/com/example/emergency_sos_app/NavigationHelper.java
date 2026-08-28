package com.example.emergency_sos_app;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class NavigationHelper {

    public static void setup(Activity activity, int currentId) {
        BottomNavigationView nav = activity.findViewById(R.id.bottomNavigation);
        if (nav == null) return;

        nav.setSelectedItemId(currentId);

        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == currentId) return true;

            Class<?> target = null;
            if (id == R.id.nav_home) target = DashboardActivity.class;
            else if (id == R.id.nav_news) target = NewsActivity.class;
            else if (id == R.id.nav_profile) target = ProfileActivity.class;
            else if (id == R.id.nav_notifications) target = AlertsActivity.class;
            else if (id == R.id.nav_family) target = FamilyActivity.class;

            if (target != null) {
                Intent intent = new Intent(activity, target);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                activity.startActivity(intent);
                activity.overridePendingTransition(0, 0);
                return true;
            }

            return false;
        });
    }

    private static void triggerFamilyAction(Activity activity) {
        // Logic moved from activities for consistency
        android.content.SharedPreferences sp = activity.getSharedPreferences("sos_profile_prefs", Activity.MODE_PRIVATE);
        String number = sp.getString("phone", "");
        if (number.isEmpty()) {
            Toast.makeText(activity, "Set up family number in Profile", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(activity, ProfileActivity.class);
            activity.startActivity(intent);
        } else {
            Intent intent = new Intent(Intent.ACTION_DIAL, android.net.Uri.parse("tel:" + number));
            activity.startActivity(intent);
        }
    }
}