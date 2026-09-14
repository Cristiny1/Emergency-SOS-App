package com.example.emergency_sos_app.repositories;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Simple provider to manage repository instances. 
 * Allows dynamic switching between Mock and Real API implementations.
 */
public class RepositoryProvider {

    private static SosRepository sosRepository;

    /**
     * Retrieves the correct repository based on user settings (Simulation Mode).
     */
    public static SosRepository getSosRepository(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("sos_profile_prefs", Context.MODE_PRIVATE);
        boolean isSimulation = prefs.getBoolean("simulation_mode", false);

        if (isSimulation) {
            // Force Mock for demonstration/testing
            if (!(sosRepository instanceof MockSosRepository)) {
                sosRepository = new MockSosRepository();
            }
        } else {
            // Return Real API for production use
            if (!(sosRepository instanceof ApiSosRepository)) {
                sosRepository = new ApiSosRepository();
            }
        }
        return sosRepository;
    }
    
    // Legacy support
    public static SosRepository getSosRepository() {
        if (sosRepository == null) {
            sosRepository = new MockSosRepository();
        }
        return sosRepository;
    }
}
