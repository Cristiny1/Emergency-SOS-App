package com.example.emergency_sos_app;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface NewsApiService {
    // Using NewsData.io as a high-quality free/freemium source for Cambodia news
    @GET("news")
    Call<NewsModel.Response> getCambodiaNews(
        @Query("apikey") String apiKey,
        @Query("country") String country,
        @Query("language") String language,
        @Query("category") String category
    );
}
