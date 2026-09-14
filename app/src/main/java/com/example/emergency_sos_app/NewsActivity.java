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

public class NewsActivity extends BaseActivity {

    private RecyclerView rvNews;
    private NewsAdapter adapter;
    private List<NewsModel.Article> articles = new ArrayList<>();
    private SwipeRefreshLayout swipeRefresh;
    private NewsApiService apiService;

    // TODO: Replace with a real API Key from NewsData.io (it's free)
    private static final String API_KEY = "pub_50228308d277717462002575796245350c388"; 

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news);
        
        initViews();
        setupRetrofit();
        
        fetchNews();
        setupNavigation(R.id.nav_news);
    }

    private void initViews() {
        rvNews = findViewById(R.id.rvNews);
        swipeRefresh = findViewById(R.id.swipeRefreshNews);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        adapter = new NewsAdapter(articles);
        rvNews.setLayoutManager(new LinearLayoutManager(this));
        rvNews.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(this::fetchNews);
    }

    private void setupRetrofit() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://newsdata.io/api/1/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(NewsApiService.class);
        
        // Initialize Translation
        TranslationManager.getInstance().initialize(this);
    }

    private void fetchNews() {
        swipeRefresh.setRefreshing(true);
        
        // Fetching for Cambodia (kh), focused on Top/Politics/Health/Environment
        apiService.getCambodiaNews(API_KEY, "kh", null, "top")
                .enqueue(new Callback<NewsModel.Response>() {
            @Override
            public void onResponse(@NonNull Call<NewsModel.Response> call, @NonNull Response<NewsModel.Response> response) {
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    articles.clear();
                    articles.addAll(response.body().results);
                    
                    // Translate if needed
                    translateNewContent();
                    
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(NewsActivity.this, "Failed to load news", Toast.LENGTH_SHORT).show();
                    loadMockData(); // Fallback if API key is invalid or quota reached
                }
            }

            @Override
            public void onFailure(@NonNull Call<NewsModel.Response> call, @NonNull Throwable t) {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(NewsActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
                loadMockData();
            }
        });
    }

    private void translateNewContent() {
        String currentLang = LanguageManager.getLanguage(this);
        if (!LanguageManager.LANG_KHMER.equals(currentLang)) return; // Only auto-translate to Khmer for now
        
        TranslationManager tm = TranslationManager.getInstance();
        if (!tm.isReady()) return;

        for (NewsModel.Article article : articles) {
            tm.translateToKhmer(this, article.title, new TranslationManager.OnTranslationListener() {
                @Override
                public void onSuccess(String translatedText) {
                    article.title = translatedText;
                    adapter.notifyDataSetChanged();
                }
                @Override public void onError(Exception e) {}
            });
            
            if (article.description != null) {
                tm.translateToKhmer(this, article.description, new TranslationManager.OnTranslationListener() {
                    @Override
                    public void onSuccess(String translatedText) {
                        article.description = translatedText;
                        adapter.notifyDataSetChanged();
                    }
                    @Override public void onError(Exception e) {}
                });
            }
        }
    }

    private void loadMockData() {
        if (!articles.isEmpty()) return; // Don't overwrite real data
        
        NewsModel.Article mock1 = new NewsModel.Article();
        mock1.title = "Ministry of Health Emergency Advisory";
        mock1.description = "New health safety protocols issued for public spaces in Phnom Penh.";
        mock1.source = "Ministry of Health";
        mock1.date = "2024-07-30";
        articles.add(mock1);
        
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
