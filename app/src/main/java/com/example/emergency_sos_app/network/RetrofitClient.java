package com.example.emergency_sos_app.network;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.HashMap;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static final String BASE_URL = "http://10.0.2.2:5000/api/";

    private static Retrofit retrofit;
    private static SosApiService apiService;
    private static SharedPreferences sharedPreferences;

    public static void init(Context context) {
        sharedPreferences = context.getSharedPreferences("auth", Context.MODE_PRIVATE);
        createRetrofitClient();
    }

    private static void createRetrofitClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .addInterceptor(new AuthInterceptor())
                .authenticator(new TokenAuthenticator())
                .build();

        Gson gson = new GsonBuilder().setLenient().create();

        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        apiService = retrofit.create(SosApiService.class);
    }

    public static SosApiService getApiService() {
        if (apiService == null) {
            throw new RuntimeException("RetrofitClient not initialized. Call init(context) first.");
        }
        return apiService;
    }

    public static void saveToken(String accessToken, String refreshToken) {
        sharedPreferences.edit()
                .putString("access_token", accessToken)
                .putString("refresh_token", refreshToken)
                .apply();
    }

    public static String getAccessToken() {
        return sharedPreferences.getString("access_token", null);
    }

    public static String getRefreshToken() {
        return sharedPreferences.getString("refresh_token", null);
    }

    public static void clearTokens() {
        sharedPreferences.edit()
                .remove("access_token")
                .remove("refresh_token")
                .apply();
    }

    private static class AuthInterceptor implements okhttp3.Interceptor {
        @Override
        public okhttp3.Response intercept(Chain chain) throws java.io.IOException {
            okhttp3.Request request = chain.request();
            String token = getAccessToken();

            if (token != null && !token.isEmpty()) {
                request = request.newBuilder()
                        .addHeader("Authorization", "Bearer " + token)
                        .build();
            }

            return chain.proceed(request);
        }
    }

    private static class TokenAuthenticator implements okhttp3.Authenticator {
        @Nullable
        @Override
        public Request authenticate(@Nullable Route route, @NonNull Response response) throws java.io.IOException {
            String refreshToken = getRefreshToken();
            if (refreshToken == null || refreshToken.isEmpty()) return null;

            Map<String, String> body = new HashMap<>();
            body.put("refreshToken", refreshToken);
            
            retrofit2.Response<com.example.emergency_sos_app.models.ApiResponse<com.example.emergency_sos_app.models.LoginData>> refreshResponse = 
                apiService.refreshToken(body).execute();

            if (refreshResponse.isSuccessful() && refreshResponse.body() != null && refreshResponse.body().success) {
                String newToken = refreshResponse.body().data.token;
                String newRefresh = refreshResponse.body().data.refreshToken;
                saveToken(newToken, newRefresh);

                return response.request().newBuilder()
                        .header("Authorization", "Bearer " + newToken)
                        .build();
            }

            clearTokens();
            return null;
        }
    }
}
