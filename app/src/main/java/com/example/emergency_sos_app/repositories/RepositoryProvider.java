package com.example.emergency_sos_app.repositories;

/**
 * Simple provider to manage repository instances. 
 * Allows easy switching between Mock and Real API implementations.
 */
public class RepositoryProvider {

    private static SosRepository sosRepository;

    public static SosRepository getSosRepository() {
        if (sosRepository == null) {
            // Initially using Mock. Later: new ApiSosRepository()
            sosRepository = new MockSosRepository();
        }
        return sosRepository;
    }
}
