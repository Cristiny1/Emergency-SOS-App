package com.example.emergency_sos_app;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import java.util.Map;

/**
 * Interface for communicating with the Laravel AI Proxy.
 */
public interface AIServiceInterface {
    @POST("ai/chat")
    Call<AIResponse> askSafetyAssistant(@Body Map<String, Object> body);
}
