package com.example.emergency_sos_app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public final class CalendarManager {
    private static final String PREFS_NAME = "safety_calendar_prefs";
    private static final String KEY_EVENTS = "calendar_events";

    public static void addEvent(Context context, String date, String description) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String data = prefs.getString(KEY_EVENTS, "{}");

        try {
            JSONObject root = new JSONObject(data);
            JSONArray array = root.optJSONArray(date);
            if (array == null) {
                array = new JSONArray();
            }
            array.put(description);
            root.put(date, array);
            prefs.edit().putString(KEY_EVENTS, root.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<String> getEvents(Context context, String date) {
        List<String> list = new ArrayList<>();
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String data = prefs.getString(KEY_EVENTS, "{}");

        try {
            JSONObject root = new JSONObject(data);
            JSONArray array = root.optJSONArray(date);
            if (array != null) {
                for (int i = 0; i < array.length(); i++) {
                    list.add(array.getString(i));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public static void deleteEvent(Context context, String date, int index) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String data = prefs.getString(KEY_EVENTS, "{}");

        try {
            JSONObject root = new JSONObject(data);
            JSONArray array = root.optJSONArray(date);
            if (array != null) {
                JSONArray newArray = new JSONArray();
                for (int i = 0; i < array.length(); i++) {
                    if (i != index) newArray.put(array.get(i));
                }
                root.put(date, newArray);
                prefs.edit().putString(KEY_EVENTS, root.toString()).apply();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
