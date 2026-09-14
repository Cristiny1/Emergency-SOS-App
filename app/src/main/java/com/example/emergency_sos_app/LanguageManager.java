package com.example.emergency_sos_app;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

/**
 * Centralized manager for application-wide language settings.
 * Supports English (en) and Khmer (km).
 */
public final class LanguageManager {
    private static final String PREFS_NAME = "sos_profile_prefs";
    private static final String KEY_LANGUAGE = "language";

    public static final String LANG_ENGLISH = "en";
    public static final String LANG_KHMER = "km";

    private LanguageManager() {}

    /**
     * Retrieves the currently saved language code.
     */
    public static String getLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LANGUAGE, LANG_ENGLISH);
    }

    /**
     * Saves the selected language and applies the locale to the application.
     * This will trigger an automatic activity recreation.
     */
    public static void setLanguage(Context context, String languageCode) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply();
        
        applyLocale(languageCode);
    }

    /**
     * Applies the saved language preference to the context.
     * Used by BaseActivity to ensure the correct locale is set on launch.
     */
    public static void apply(Context context) {
        if (context == null) return;
        try {
            String savedLanguage = getLanguage(context);
            LocaleListCompat currentLocales = AppCompatDelegate.getApplicationLocales();

            // Only apply if the current language differs from the saved preference
            if (currentLocales.isEmpty() || !savedLanguage.equals(currentLocales.get(0).getLanguage())) {
                applyLocale(savedLanguage);
            }
        } catch (Exception e) {
            android.util.Log.e("LangManager", "Failed to apply language", e);
        }
    }

    private static void applyLocale(String languageCode) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageCode));
    }

    public static boolean isKhmer(Context context) {
        return LANG_KHMER.equals(getLanguage(context));
    }

    /** Returns the flag icon based on the current language */
    public static int getFlagDrawable(Context context) {
        return isKhmer(context) ? R.drawable.cam_flag : R.drawable.en_flag;
    }
}
