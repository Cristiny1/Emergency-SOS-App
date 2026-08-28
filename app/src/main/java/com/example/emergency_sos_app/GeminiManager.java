package com.example.emergency_sos_app;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Manages AI interactions for safety assistance via a secure Laravel backend proxy.
 */
public class GeminiManager {

    private static final String BACKEND_BASE_URL = "http://10.0.2.2:8000/api/"; // Replace with real server URL
    private static GeminiManager instance;
    private final AIServiceInterface apiService;

    public interface GeminiCallback {
        void onSuccess(AIResponse response);
        void onError(Throwable throwable);
    }

    private GeminiManager() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BACKEND_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(AIServiceInterface.class);
    }

    public static synchronized GeminiManager getInstance() {
        if (instance == null) {
            instance = new GeminiManager();
        }
        return instance;
    }

    public void askSafetyAssistant(String prompt, String language, GeminiCallback callback) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", prompt);
        body.put("language", language);
        // Location could be added here if needed for hospitals, etc.

        apiService.askSafetyAssistant(body).enqueue(new Callback<AIResponse>() {
            @Override
            public void onResponse(Call<AIResponse> call, Response<AIResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(new Exception("Backend error: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<AIResponse> call, Throwable t) {
                callback.onError(t);
            }
        });
    }
}
