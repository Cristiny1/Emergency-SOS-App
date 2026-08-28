package com.example.emergency_sos_app;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class NewsModel {

    public static class Response {
        @SerializedName("results")
        public List<Article> results;
    }

    public static class Article {
        @SerializedName("title")
        public String title;
        
        @SerializedName("description")
        public String description;
        
        @SerializedName("source_id")
        public String source;
        
        @SerializedName("pubDate")
        public String date;

        @SerializedName("link")
        public String link;
    }
}
