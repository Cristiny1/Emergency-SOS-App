package com.example.emergency_sos_app.network;

import com.example.emergency_sos_app.models.ApiResponse;
import com.example.emergency_sos_app.models.CreateSosRequest;
import com.example.emergency_sos_app.models.LoginData;
import com.example.emergency_sos_app.models.LoginRequest;
import com.example.emergency_sos_app.models.SosEvent;

import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface SosApiService {

    @POST("login")
    Call<ApiResponse<LoginData>> login(@Body LoginRequest request);

    @POST("sos")
    Call<ApiResponse<Void>> createSos(@Body CreateSosRequest request);

    @GET("sos/{id}")
    Call<ApiResponse<SosEvent>> getSosStatus(@Path("id") String id);

    @POST("sos/{id}/cancel")
    Call<ApiResponse<Void>> cancelSos(@Path("id") String id);

    @Multipart
    @POST("reports")
    Call<ApiResponse<Void>> createReport(
            @Part("category") RequestBody category,
            @Part("severity") RequestBody severity,
            @Part("description") RequestBody description,
            @Part("latitude") RequestBody latitude,
            @Part("longitude") RequestBody longitude,
            @Part MultipartBody.Part photo,
            @Part MultipartBody.Part voice
    );

    @POST("auth/refresh")
    Call<ApiResponse<LoginData>> refreshToken(@Body Map<String, String> body);
}
