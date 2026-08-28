package com.example.emergency_sos_app;

/**
 * Model for chat messages, now supporting AI metadata and action recommendations.
 */
public class ChatMessage {
    private String text;
    private boolean isUser;
    private boolean isTyping;
    
    // AI Metadata
    private String category;
    private String severity;
    private String recommendedAction;
    private boolean showSOS;
    private boolean showReport;

    public ChatMessage(String text, boolean isUser) {
        this.text = text;
        this.isUser = isUser;
        this.isTyping = false;
        this.category = "NORMAL";
        this.severity = "NORMAL";
    }

    public ChatMessage(AIResponse response) {
        this.text = response.message;
        this.isUser = false;
        this.isTyping = false;
        this.category = response.category;
        this.severity = response.severity;
        this.recommendedAction = response.recommendedAction;
        this.showSOS = response.showSOS;
        this.showReport = response.showReport;
    }

    public static ChatMessage typing() {
        ChatMessage m = new ChatMessage("...", false);
        m.isTyping = true;
        return m;
    }

    public String getText() { return text; }
    public boolean isUser() { return isUser; }
    public boolean isTyping() { return isTyping; }
    public String getCategory() { return category; }
    public String getSeverity() { return severity; }
    public String getRecommendedAction() { return recommendedAction; }
    public boolean isShowSOS() { return showSOS; }
    public boolean isShowReport() { return showReport; }
}
