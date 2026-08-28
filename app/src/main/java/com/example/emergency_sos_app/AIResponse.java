package com.example.emergency_sos_app;

import com.google.gson.annotations.SerializedName;

/**
 * Structured model for the AI's intelligent responses.
 */
public class AIResponse {
    @SerializedName("success")
    public boolean success;

    @SerializedName("message")
    public String message;

    @SerializedName("language")
    public String language;

    @SerializedName("category")
    public String category; // MEDICAL, FIRE, POLICE, etc.

    @SerializedName("severity")
    public String severity; // CRITICAL, HIGH, MEDIUM, LOW, NORMAL

    @SerializedName("recommendedAction")
    public String recommendedAction; // ACTIVATE_SOS, CALL_POLICE, etc.

    @SerializedName("showSOS")
    public boolean showSOS;

    @SerializedName("showEmergencyCall")
    public boolean showEmergencyCall;

    @SerializedName("showLocation")
    public boolean showLocation;

    @SerializedName("showReport")
    public boolean showReport;

    public AIResponse(String message) {
        this.message = message;
        this.success = true;
        this.category = "NORMAL";
        this.severity = "NORMAL";
    }
}
