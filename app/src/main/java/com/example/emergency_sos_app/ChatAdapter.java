package com.example.emergency_sos_app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private final List<ChatMessage> messages;
    private final OnActionClickListener actionListener;

    public interface OnActionClickListener {
        void onSOSClick();
        void onCallClick(String category);
        void onReportClick(String category);
    }

    public ChatAdapter(List<ChatMessage> messages, OnActionClickListener listener) {
        this.messages = messages;
        this.actionListener = listener;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        
        if (message.isUser()) {
            holder.userContainer.setVisibility(View.VISIBLE);
            holder.botContainer.setVisibility(View.GONE);
            holder.tvUser.setText(message.getText());
        } else {
            holder.botContainer.setVisibility(View.VISIBLE);
            holder.userContainer.setVisibility(View.GONE);
            holder.tvBot.setText(message.isTyping() ? "AI Assistant is thinking..." : message.getText());
            
            // AI Actions
            if (!message.isTyping() && (message.isShowSOS() || message.getRecommendedAction() != null)) {
                holder.layoutActions.setVisibility(View.VISIBLE);
                
                holder.btnSOS.setVisibility(message.isShowSOS() ? View.VISIBLE : View.GONE);
                holder.btnSOS.setOnClickListener(v -> actionListener.onSOSClick());
                
                boolean showCall = message.getRecommendedAction() != null && message.getRecommendedAction().startsWith("CALL");
                holder.btnCall.setVisibility(showCall ? View.VISIBLE : View.GONE);
                if (showCall) {
                    holder.btnCall.setText("📞 CALL " + message.getCategory());
                    holder.btnCall.setOnClickListener(v -> actionListener.onCallClick(message.getCategory()));
                }
            } else {
                holder.layoutActions.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        View botContainer, userContainer, layoutActions;
        TextView tvBot, tvUser;
        Button btnSOS, btnCall;

        ChatViewHolder(View itemView) {
            super(itemView);
            botContainer = itemView.findViewById(R.id.botContainer);
            userContainer = itemView.findViewById(R.id.userContainer);
            layoutActions = itemView.findViewById(R.id.layoutAiActions);
            tvBot = itemView.findViewById(R.id.tvBotMessage);
            tvUser = itemView.findViewById(R.id.tvUserMessage);
            btnSOS = itemView.findViewById(R.id.btnAiActionSOS);
            btnCall = itemView.findViewById(R.id.btnAiActionCall);
        }
    }
}
