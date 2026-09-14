package com.example.emergency_sos_app;

import android.app.Application;
import com.example.emergency_sos_app.network.RetrofitClient;

public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        RetrofitClient.init(this);
    }
}