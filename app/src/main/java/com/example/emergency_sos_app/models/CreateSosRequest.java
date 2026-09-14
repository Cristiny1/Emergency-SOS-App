package com.example.emergency_sos_app.models;

public class CreateSosRequest {
    public String emergencyType;
    public String description;
    public Double latitude;
    public Double longitude;

    public CreateSosRequest(String emergencyType, String description, Double latitude, Double longitude) {
        this.emergencyType = emergencyType;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
