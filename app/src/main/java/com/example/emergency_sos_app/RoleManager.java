package com.example.emergency_sos_app;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.Locale;

public final class RoleManager {

    public static final String KEY_SELECTED_ROLE =
            "selected_user_role";

    public static final String KEY_AUTHENTICATED_ROLE =
            "authenticated_user_role";

    private static final String PREFS_NAME =
            "sos_profile_prefs";

    public static final String ROLE_USER = "user";
    public static final String ROLE_FAMILY_GUARDIAN =
            "family_guardian";
    public static final String ROLE_EMERGENCY_RESPONDER =
            "emergency_responder";
    public static final String ROLE_ADMIN = "admin";

    private RoleManager() {
        // Prevent instantiation
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(
                        PREFS_NAME,
                        Context.MODE_PRIVATE
                );
    }

    // Selected role: public registration only
    public static String getSelectedRole(Context context) {
        if (context == null) {
            return "";
        }

        String role = getPrefs(context).getString(
                KEY_SELECTED_ROLE, ""
        );

        return role == null ? "" : role;
    }

    public static boolean hasSelectedRole(Context context) {
        return ROLE_USER.equals(getSelectedRole(context));
    }

    public static boolean isSelectableRole(String role) {
        return ROLE_USER.equals(normalizeRole(role));
    }

    public static boolean setSelectedRole(
            Context context, String role) {

        if (context == null || !isSelectableRole(role)) {
            return false;
        }

        getPrefs(context).edit()
                .putString(KEY_SELECTED_ROLE, ROLE_USER)
                .apply();

        return true;
    }

    // Authenticated role: set only after server verification
    public static String getAuthenticatedRole(Context context) {
        if (context == null) {
            return "";
        }

        String role = getPrefs(context).getString(
                KEY_AUTHENTICATED_ROLE, ""
        );

        return role == null ? "" : role;
    }

    public static boolean hasResponderAccess(Context context) {
        return ROLE_EMERGENCY_RESPONDER.equals(
                getAuthenticatedRole(context)
        );
    }

    public static boolean hasAdminAccess(Context context) {
        return ROLE_ADMIN.equals(
                getAuthenticatedRole(context)
        );
    }

    /**
     * Call this only after a trusted backend response
     * confirms the authenticated account's role.
     */
    public static void setAuthenticatedRole(
            Context context, String role) {

        if (context == null) {
            return;
        }

        String normalizedRole = normalizeRole(role);

        // Never grant privileged roles from arbitrary input.
        if (!ROLE_USER.equals(normalizedRole)
                && !ROLE_FAMILY_GUARDIAN.equals(normalizedRole)
                && !ROLE_EMERGENCY_RESPONDER.equals(normalizedRole)
                && !ROLE_ADMIN.equals(normalizedRole)) {

            clearAuthenticatedRole(context);
            return;
        }

        getPrefs(context).edit()
                .putString(
                        KEY_AUTHENTICATED_ROLE,
                        normalizedRole
                )
                .apply();
    }

    public static void clearAuthenticatedRole(Context context) {
        if (context == null) {
            return;
        }

        getPrefs(context).edit()
                .remove(KEY_AUTHENTICATED_ROLE)
                .apply();
    }

    private static String normalizeRole(String role) {
        if (TextUtils.isEmpty(role)) {
            return "";
        }

        String normalized = role.trim().toLowerCase(Locale.ROOT);

        switch (normalized) {
            case "citizen":
            case "tourist":
                return ROLE_USER;

            case "responder":
                return ROLE_EMERGENCY_RESPONDER;

            default:
                return normalized;
        }
    }

    public static String getDisplayLabel(
            Context context, String role) {

        if (context == null) {
            return "";
        }

        switch (normalizeRole(role)) {
            case ROLE_FAMILY_GUARDIAN:
                return context.getString(
                        R.string.role_family_guardian_label
                );

            case ROLE_EMERGENCY_RESPONDER:
                return context.getString(
                        R.string.role_emergency_responder_label
                );

            case ROLE_ADMIN:
                return context.getString(
                        R.string.role_admin_label
                );

            case ROLE_USER:
            default:
                return context.getString(
                        R.string.role_user_label
                );
        }
    }

    public static String getDescription(
            Context context, String role) {

        if (context == null) {
            return "";
        }

        switch (normalizeRole(role)) {
            case ROLE_FAMILY_GUARDIAN:
                return context.getString(
                        R.string.role_family_guardian_desc
                );

            case ROLE_EMERGENCY_RESPONDER:
                return context.getString(
                        R.string.role_emergency_responder_desc
                );

            case ROLE_ADMIN:
                return context.getString(
                        R.string.role_admin_desc
                );

            case ROLE_USER:
            default:
                return context.getString(
                        R.string.role_user_desc
                );
        }
    }
}
