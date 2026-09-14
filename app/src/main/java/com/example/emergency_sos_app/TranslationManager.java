package com.example.emergency_sos_app;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Singleton managing Google ML Kit Translation with persistent caching.
 */
public final class TranslationManager {
    private static final String TAG = "TranslationManager";
    private static final String PREFS_NAME = "translation_cache_prefs";
    private static TranslationManager instance;
    
    private Translator enToKmTranslator;
    private Translator kmToEnTranslator;
    private boolean isModelsReady = false;
    
    // In-memory cache for speed, backed by SharedPreferences for persistence
    private final Map<String, String> translationCache = new HashMap<>();

    public interface OnTranslationListener {
        void onSuccess(String translatedText);
        void onError(Exception e);
    }

    private TranslationManager() {}

    public static synchronized TranslationManager getInstance() {
        if (instance == null) {
            instance = new TranslationManager();
        }
        return instance;
    }

    public void initialize(Context context) {
        if (isModelsReady) return;

        // Load cache in background to prevent UI hang
        new Thread(() -> loadCache(context)).start();

        String enTag = TranslateLanguage.ENGLISH;
        String kmTag = "km"; 

        TranslatorOptions enToKmOptions = new TranslatorOptions.Builder()
                .setSourceLanguage(enTag)
                .setTargetLanguage(kmTag)
                .build();
        enToKmTranslator = Translation.getClient(enToKmOptions);

        TranslatorOptions kmToEnOptions = new TranslatorOptions.Builder()
                .setSourceLanguage(kmTag)
                .setTargetLanguage(enTag)
                .build();
        kmToEnTranslator = Translation.getClient(kmToEnOptions);

        DownloadConditions conditions = new DownloadConditions.Builder()
                .requireWifi()
                .build();

        List<Task<Void>> tasks = new ArrayList<>();
        tasks.add(enToKmTranslator.downloadModelIfNeeded(conditions));
        tasks.add(kmToEnTranslator.downloadModelIfNeeded(conditions));

        Tasks.whenAll(tasks)
                .addOnSuccessListener(unused -> {
                    isModelsReady = true;
                    Log.d(TAG, "Translation models downloaded and ready.");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to download translation models: " + e.getMessage());
                });
    }

    public void translateToKhmer(Context context, String text, OnTranslationListener listener) {
        if (text == null || text.trim().isEmpty()) {
            listener.onSuccess("");
            return;
        }

        // 1. Check Cache
        if (translationCache.containsKey(text)) {
            listener.onSuccess(translationCache.get(text));
            return;
        }

        if (!isModelsReady || enToKmTranslator == null) {
            listener.onError(new Exception("Translation models not ready"));
            return;
        }

        // 2. Run Translation
        enToKmTranslator.translate(text)
                .addOnSuccessListener(translatedText -> {
                    translationCache.put(text, translatedText);
                    saveToCache(context, text, translatedText);
                    listener.onSuccess(translatedText);
                })
                .addOnFailureListener(listener::onError);
    }

    public void translate(Context context, String text, String targetLang, OnTranslationListener listener) {
        if (LanguageManager.LANG_KHMER.equals(targetLang)) {
            translateToKhmer(context, text, listener);
        } else {
            // Reversing logic for English if needed, currently prioritized for Khmer
            listener.onSuccess(text);
        }
    }

    private void loadCache(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Map<String, ?> allEntries = prefs.getAll();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            translationCache.put(entry.getKey(), entry.getValue().toString());
        }
    }

    private void saveToCache(Context context, String original, String translated) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(original, translated).apply();
    }

    public boolean isReady() {
        return isModelsReady;
    }
}
