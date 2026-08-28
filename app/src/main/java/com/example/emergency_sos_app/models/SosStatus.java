package com.example.emergency_sos_app.models;

/**
 * State machine enum for the life-cycle of an SOS emergency.
 */
public enum SosStatus {
    IDLE,
    ACTIVATING,
    PENDING,
    SENT,
    ACKNOWLEDGED,
    RESPONDER_ASSIGNED,
    RESPONDER_EN_ROUTE,
    ARRIVED,
    RESOLVED,
    CANCELLED,
    FAILED
}
