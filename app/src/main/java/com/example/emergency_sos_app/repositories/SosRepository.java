package com.example.emergency_sos_app.repositories;

import com.example.emergency_sos_app.models.SosEvent;
import com.example.emergency_sos_app.models.SosStatus;

/**
 * Interface defining all SOS operations.
 */
public interface SosRepository {

    void createSos(SosEvent sos, SosCallback callback);

    void cancelSos(String sosId, SosCallback callback);

    void getSosStatus(String sosId, SosCallback callback);

    interface SosCallback {
        void onStatusChanged(SosStatus status);
        void onError(String message);
    }
}
