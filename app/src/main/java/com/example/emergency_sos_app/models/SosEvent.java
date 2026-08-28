package com.example.emergency_sos_app.models;

/**
 * Domain model representing a single SOS emergency event.
 */
public class SosEvent {
    private String sosId;
    private String userId;
    private String type; // MEDICAL, FIRE, POLICE
    private double latitude;
    private double longitude;
    private float accuracy;
    private SosStatus status;
    private long timestamp;

    public SosEvent(String sosId, String userId, String type, double latitude, double longitude, float accuracy) {
        this.sosId = sosId;
        this.userId = userId;
        this.type = type;
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracy = accuracy;
        this.status = SosStatus.PENDING;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getSosId() { return sosId; }
    public String getUserId() { return userId; }
    public String getType() { return type; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public float getAccuracy() { return accuracy; }
    public SosStatus getStatus() { return status; }
    public void setStatus(SosStatus status) { this.status = status; }
    public long getTimestamp() { return timestamp; }
}
