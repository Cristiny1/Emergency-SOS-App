package com.example.emergency_sos_app;

import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AlertsActivity extends BaseActivity {

    private RecyclerView rvAlerts;
    private NewsAdapter adapter; // Reusing NewsAdapter for now
    private List<NewsModel.Article> alerts = new ArrayList<>();
    private SwipeRefreshLayout swipeRefresh;
    private NewsApiService apiService;
    private static final String API_KEY = "pub_50228308d277717462002575796245350c388";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alerts);

        initViews();
        setupRetrofit();
        
        fetchAlerts();
        setupNavigation(R.id.nav_notifications);
    }

    private void initViews() {
        rvAlerts = findViewById(R.id.rvAlerts);
        swipeRefresh = findViewById(R.id.swipeRefreshAlerts);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        adapter = new NewsAdapter(alerts);
        rvAlerts.setLayoutManager(new LinearLayoutManager(this));
        rvAlerts.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(this::fetchAlerts);
    }

    private void setupRetrofit() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://newsdata.io/api/1/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(NewsApiService.class);
    }

    private void fetchAlerts() {
        swipeRefresh.setRefreshing(true);
        // Specifically filtering for environment/health/science which often contain emergency alerts
        apiService.getCambodiaNews(API_KEY, "kh", "en,kh", "environment,health")
                .enqueue(new Callback<NewsModel.Response>() {
            @Override
            public void onResponse(@NonNull Call<NewsModel.Response> call, @NonNull Response<NewsModel.Response> response) {
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    alerts.clear();
                    alerts.addAll(response.body().results);
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(AlertsActivity.this, "No active safety alerts", Toast.LENGTH_SHORT).show();
                    loadMockAlerts();
                }
            }

            @Override
            public void onFailure(@NonNull Call<NewsModel.Response> call, @NonNull Throwable t) {
                swipeRefresh.setRefreshing(false);
                loadMockAlerts();
            }
        });
    }

    private void loadMockAlerts() {
        if (!alerts.isEmpty()) return;
        NewsModel.Article alert = new NewsModel.Article();
        alert.title = "Local Safety Update";
        alert.description = "All emergency services are operational. Stay alert for weather updates.";
        alert.source = "Emergency SOS System";
        alert.date = "Just Now";
        alerts.add(alert);
        adapter.notifyDataSetChanged();
    }

    private void setupEdgeToEdge() {
        View toolbar = findViewById(R.id.toolbar);
        View bottomNav = findViewById(R.id.bottomNavigation);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            int bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;

            if (toolbar != null) {
                toolbar.setPadding(0, top, 0, 0);
            }
            if (bottomNav != null) {
                RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) bottomNav.getLayoutParams();
                lp.bottomMargin = (int) (16 * getResources().getDisplayMetrics().density) + bottom;
                bottomNav.setLayoutParams(lp);
            }
            return WindowInsetsCompat.CONSUMED;
        });
    }
}
