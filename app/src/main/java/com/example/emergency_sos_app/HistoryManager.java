package com.example.emergency_sos_app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public final class HistoryManager {
    private static final String PREFS_NAME = "sos_history_prefs";
    private static final String KEY_HISTORY = "history_list";

    public static void saveEvent(Context context, String type, String location) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String currentHistory = prefs.getString(KEY_HISTORY, "[]");

        try {
            JSONArray array = new JSONArray(currentHistory);
            JSONObject event = new JSONObject();
            event.put("type", type);
            event.put("location", location);
            event.put("timestamp", System.currentTimeMillis());

            // Add to start (newest first)
            JSONArray newArray = new JSONArray();
            newArray.put(event);
            for (int i = 0; i < array.length(); i++) {
                if (newArray.length() >= 50) break; // Keep last 50
                newArray.put(array.get(i));
            }

            prefs.edit().putString(KEY_HISTORY, newArray.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<HistoryEvent> getHistory(Context context) {
        List<HistoryEvent> list = new ArrayList<>();
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String data = prefs.getString(KEY_HISTORY, "[]");

        try {
            JSONArray array = new JSONArray(data);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                list.add(new HistoryEvent(
                        obj.getString("type"),
                        obj.getString("location"),
                        obj.getLong("timestamp")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public static void clear(Context context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply();
    }

    public static void deleteEvent(Context context, long timestamp) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String currentHistory = prefs.getString(KEY_HISTORY, "[]");

        try {
            JSONArray array = new JSONArray(currentHistory);
            JSONArray newArray = new JSONArray();
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                if (obj.getLong("timestamp") != timestamp) {
                    newArray.put(obj);
                }
            }
            prefs.edit().putString(KEY_HISTORY, newArray.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class HistoryEvent {
        public String type, location;
        public long timestamp;

        public HistoryEvent(String t, String l, long ts) {
            type = t; location = l; timestamp = ts;
        }
    }
}
