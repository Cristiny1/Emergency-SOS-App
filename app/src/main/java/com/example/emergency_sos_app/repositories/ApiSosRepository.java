package com.example.emergency_sos_app.repositories;

import android.util.Log;

import com.example.emergency_sos_app.models.ApiResponse;
import com.example.emergency_sos_app.models.CreateSosRequest;
import com.example.emergency_sos_app.models.SosEvent;
import com.example.emergency_sos_app.models.SosStatus;
import com.example.emergency_sos_app.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ApiSosRepository implements SosRepository {

    private static final String TAG = "ApiSosRepo";

    @Override
    public void createSos(SosEvent sos, SosCallback callback) {
        CreateSosRequest request = new CreateSosRequest(
                sos.getType(),
                "Emergency SOS Triggered from Mobile",
                sos.getLatitude(),
                sos.getLongitude()
        );

        RetrofitClient.getApiService().createSos(request).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().success) {
                        callback.onStatusChanged(SosStatus.SENT);
                    } else {
                        callback.onError(response.body().message);
                    }
                } else {
                    callback.onError("Server error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                callback.onError("Connection failed. Signal will retry.");
                Log.e(TAG, "createSos failure", t);
            }
        });
    }

    @Override
    public void cancelSos(String sosId, SosCallback callback) {
        RetrofitClient.getApiService().cancelSos(sosId).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    callback.onStatusChanged(SosStatus.CANCELLED);
                } else {
                    callback.onError("Failed to cancel on server.");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                callback.onError("Network error during cancellation.");
            }
        });
    }

    @Override
    public void getSosStatus(String sosId, SosCallback callback) {
        // We call the API to get the latest authoritative state
        RetrofitClient.getApiService().getSosStatus(sosId).enqueue(new Callback<ApiResponse<SosEvent>>() {
            @Override
            public void onResponse(Call<ApiResponse<SosEvent>> call, Response<ApiResponse<SosEvent>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    SosEvent event = response.body().data;
                    if (event != null) {
                        Log.d(TAG, "Authoritative status for " + sosId + ": " + event.getStatus());
                        callback.onStatusChanged(event.getStatus());
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<SosEvent>> call, Throwable t) {
                Log.w(TAG, "getSosStatus authoritative fetch failed", t);
            }
        });
    }
}
