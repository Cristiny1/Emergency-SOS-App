package com.example.emergency_sos_app;

import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Map;

public class FirebaseLocationManager {

    public interface LocationUpdateListener {
        void onLocationUpdate(double lat, double lng, float bearing);
        void onStatusUpdate(String status, String agentName);
    }

    private DatabaseReference dbRef;

    public FirebaseLocationManager(String requestId) {
        try {
            // This will fail if google-services.json is missing or Firebase is not initialized
            this.dbRef = FirebaseDatabase.getInstance().getReference("sos_requests").child(requestId);
        } catch (Exception e) {
            Log.e("Firebase", "Firebase Database not initialized: " + e.getMessage());
            this.dbRef = null;
        }
    }

    public void updateRequest(Map<String, Object> data) {
        if (dbRef != null) {
            dbRef.updateChildren(data);
        }
    }

    public void startListening(LocationUpdateListener listener) {
        if (dbRef == null) return;
        
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String status = snapshot.child("status").getValue(String.class);
                    String agentName = snapshot.child("agentName").getValue(String.class);
                    listener.onStatusUpdate(status, agentName);

                    Double lat = snapshot.child("agentLat").getValue(Double.class);
                    Double lng = snapshot.child("agentLng").getValue(Double.class);
                    Float bearing = snapshot.child("agentBearing").getValue(Float.class);

                    if (lat != null && lng != null) {
                        listener.onLocationUpdate(lat, lng, bearing != null ? bearing : 0);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }
}