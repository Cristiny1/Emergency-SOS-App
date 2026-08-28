package com.example.emergency_sos_app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.NewsViewHolder> {

    private final List<NewsModel.Article> articles;

    public NewsAdapter(List<NewsModel.Article> articles) {
        this.articles = articles;
    }

    @NonNull
    @Override
    public NewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_news_card, parent, false);
        return new NewsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NewsViewHolder holder, int position) {
        NewsModel.Article article = articles.get(position);
        holder.tvTitle.setText(article.title);
        holder.tvDescription.setText(article.description != null ? article.description : "No description available.");
        holder.tvSource.setText(article.source != null ? "Source: " + article.source : "Official Announcement");
        holder.tvDate.setText(article.date);
    }

    @Override
    public int getItemCount() {
        return articles.size();
    }

    static class NewsViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDescription, tvSource, tvDate;

        NewsViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvNewsTitle);
            tvDescription = itemView.findViewById(R.id.tvNewsDescription);
            tvSource = itemView.findViewById(R.id.tvNewsSource);
            tvDate = itemView.findViewById(R.id.tvNewsDate);
        }
    }
}
