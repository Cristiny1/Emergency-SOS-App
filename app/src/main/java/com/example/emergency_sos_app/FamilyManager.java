package com.example.emergency_sos_app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public final class FamilyManager {
    private static final String PREFS_NAME = "family_circle_prefs";
    private static final String KEY_MEMBERS = "members_list";

    public static void saveMember(Context context, FamilyMember member) {
        List<FamilyMember> members = getMembers(context);
        boolean found = false;
        for (int i = 0; i < members.size(); i++) {
            if (members.get(i).id.equals(member.id)) {
                members.set(i, member);
                found = true;
                break;
            }
        }
        if (!found) {
            members.add(member);
        }
        saveList(context, members);
    }

    public static List<FamilyMember> getMembers(Context context) {
        List<FamilyMember> list = new ArrayList<>();
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String data = prefs.getString(KEY_MEMBERS, "[]");

        try {
            JSONArray array = new JSONArray(data);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                list.add(new FamilyMember(
                        obj.getString("id"),
                        obj.getString("name"),
                        obj.getString("phone"),
                        obj.optString("relationship", "Other")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public static void deleteMember(Context context, String id) {
        List<FamilyMember> members = getMembers(context);
        for (int i = 0; i < members.size(); i++) {
            if (members.get(i).id.equals(id)) {
                members.remove(i);
                break;
            }
        }
        saveList(context, members);
    }

    private static void saveList(Context context, List<FamilyMember> members) {
        try {
            JSONArray array = new JSONArray();
            for (FamilyMember m : members) {
                JSONObject obj = new JSONObject();
                obj.put("id", m.id);
                obj.put("name", m.name);
                obj.put("phone", m.phone);
                obj.put("relationship", m.relationship);
                array.put(obj);
            }
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit().putString(KEY_MEMBERS, array.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class FamilyMember {
        public String id, name, phone, relationship;
        public FamilyMember(String id, String name, String phone, String relationship) {
            this.id = id;
            this.name = name;
            this.phone = phone;
            this.relationship = relationship;
        }
    }
}
