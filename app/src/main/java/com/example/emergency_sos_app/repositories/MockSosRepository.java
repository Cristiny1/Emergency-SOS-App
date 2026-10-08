package com.example.emergency_sos_app.repositories;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.example.emergency_sos_app.models.SosEvent;
import com.example.emergency_sos_app.models.SosStatus;

/**
 * Mock implementation of SosRepository for development and testing.
 * Simulates a real backend with delays and status transitions.
 */
public class MockSosRepository implements SosRepository {

    private Handler handler;
    private SosEvent lastEvent;
    private SosCallback activeCallback;

    private Handler getHandler() {
        if (handler == null) {
            handler = new Handler(Looper.getMainLooper());
        }
        return handler;
    }

    @Override
    public void createSos(SosEvent sos, SosCallback callback) {
        Log.d("MockRepo", "Creating SOS: " + sos.getSosId());
        this.lastEvent = sos;
        this.activeCallback = callback;
        
        // Start the rescue simulation logic
        simulateProgression();
    }

    private void simulateProgression() {
        Handler handler = getHandler();

        // Step 1: Pending (Local)
        updateStatus(SosStatus.PENDING);

        // Step 2: Simulate network sending (500ms)
        handler.postDelayed(() -> {
            updateStatus(SosStatus.SENT);
            
            // Step 3: Server Acknowledged (1s later)
            handler.postDelayed(() -> {
                updateStatus(SosStatus.ACKNOWLEDGED);
                
                // Step 4: Responder Assigned (1.5s later)
                handler.postDelayed(() -> {
                    updateStatus(SosStatus.RESPONDER_ASSIGNED);
                    
                    // Step 5: En Route (2s later)
                    handler.postDelayed(() -> {
                        updateStatus(SosStatus.RESPONDER_EN_ROUTE);
                        
                        // Step 6: Arrived (2s later)
                        handler.postDelayed(() -> {
                            updateStatus(SosStatus.ARRIVED);
                        }, 2000);
                    }, 2000);
                }, 1500);
            }, 1000);
        }, 500);
    }

    private void updateStatus(SosStatus status) {
        if (lastEvent != null) {
            lastEvent.setStatus(status);
            Log.d("MockRepo", "Status Updated: " + status);
        }
        if (activeCallback != null) {
            try {
                // Ensure UI notifications happen on the main thread if coming from handler
                getHandler().post(() -> activeCallback.onStatusChanged(status));
            } catch (Exception e) {
                Log.e("MockRepo", "Callback notification error: " + e.getMessage());
            }
        }
    }

    @Override
    public void cancelSos(String sosId, SosCallback callback) {
        Log.d("MockRepo", "Cancelling SOS: " + sosId);
        this.activeCallback = callback;
        getHandler().removeCallbacksAndMessages(null); // Stop any pending mock updates
        updateStatus(SosStatus.CANCELLED);
    }

    @Override
    public void getSosStatus(String sosId, SosCallback callback) {
        if (lastEvent != null && lastEvent.getSosId().equals(sosId)) {
            Log.d("MockRepo", "Re-binding SOS Status: " + lastEvent.getStatus());
            this.activeCallback = callback;
            callback.onStatusChanged(lastEvent.getStatus());
        } else {
            Log.d("MockRepo", "No active SOS found for ID: " + sosId);
        }
    }
}
