package com.example.emergency_sos_app.repositories;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Simple provider to manage repository instances. 
 * Allows dynamic switching between Mock and Real API implementations.
 */
public class RepositoryProvider {

    private static SosRepository sosRepository;
    private static Boolean repositorySimulationMode;

    /**
     * Creates the correct repository instance based on the current mode.
     * This avoids stale cached instances when the user toggles simulation mode.
     */
    public static SosRepository createRepositoryForMode(boolean isSimulation) {
        return isSimulation ? new MockSosRepository() : new ApiSosRepository();
    }

    /**
     * Retrieves the correct repository based on user settings (Simulation Mode).
     */
    public static synchronized SosRepository getSosRepository(Context context) {
        if (context == null) {
            return getSosRepository();
        }

        Context appContext = context.getApplicationContext();
        SharedPreferences prefs = appContext.getSharedPreferences("sos_profile_prefs", Context.MODE_PRIVATE);
        boolean isSimulation = prefs.getBoolean("simulation_mode", false);

        if (sosRepository == null || repositorySimulationMode == null || repositorySimulationMode != isSimulation) {
            sosRepository = createRepositoryForMode(isSimulation);
            repositorySimulationMode = isSimulation;
        }
        return sosRepository;
    }

    // Legacy support
    public static synchronized SosRepository getSosRepository() {
        if (sosRepository == null) {
            sosRepository = new MockSosRepository();
            repositorySimulationMode = true;
        }
        return sosRepository;
    }
}
