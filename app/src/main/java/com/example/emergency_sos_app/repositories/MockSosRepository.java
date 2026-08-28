package com.example.emergency_sos_app.repositories;

import android.os.Handler;
import android.os.Looper;
import com.example.emergency_sos_app.models.SosEvent;
import com.example.emergency_sos_app.models.SosStatus;

/**
 * Mock implementation of SosRepository for development and testing.
 * Simulates a real backend with delays and status transitions.
 */
public class MockSosRepository implements SosRepository {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private SosEvent lastEvent;
    private SosCallback activeCallback;

    @Override
    public void createSos(SosEvent sos, SosCallback callback) {
        this.lastEvent = sos;
        this.activeCallback = callback;
        
        simulateProgression(callback);
    }

    private void simulateProgression(SosCallback callback) {
        // Step 1: Pending (Local)
        callback.onStatusChanged(SosStatus.PENDING);

        // Step 2: Simulate network sending (1s)
        handler.postDelayed(() -> {
            updateStatus(SosStatus.SENT, callback);
            
            // Step 3: Server Acknowledged (2s later)
            handler.postDelayed(() -> {
                updateStatus(SosStatus.ACKNOWLEDGED, callback);
                
                // Step 4: Responder Assigned (3s later)
                handler.postDelayed(() -> {
                    updateStatus(SosStatus.RESPONDER_ASSIGNED, callback);
                    
                    // Step 5: En Route (4s later)
                    handler.postDelayed(() -> {
                        updateStatus(SosStatus.RESPONDER_EN_ROUTE, callback);
                    }, 4000);
                }, 3000);
            }, 2000);
        }, 1000);
    }

    private void updateStatus(SosStatus status, SosCallback callback) {
        if (lastEvent != null) lastEvent.setStatus(status);
        if (callback != null) callback.onStatusChanged(status);
    }

    @Override
    public void cancelSos(String sosId, SosCallback callback) {
        handler.removeCallbacksAndMessages(null); // Stop any pending mock updates
        updateStatus(SosStatus.CANCELLED, callback);
    }

    @Override
    public void getSosStatus(String sosId, SosCallback callback) {
        if (lastEvent != null && lastEvent.getSosId().equals(sosId)) {
            // If we are already simulating, just reconnect the callback
            this.activeCallback = callback;
            callback.onStatusChanged(lastEvent.getStatus());
        }
    }
}
