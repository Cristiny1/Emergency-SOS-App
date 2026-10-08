package com.example.emergency_sos_app.repositories;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RepositoryProviderTest {

    @Test
    public void createRepositoryForMode_returnsMockInSimulationMode() {
        SosRepository repository = RepositoryProvider.createRepositoryForMode(true);
        assertTrue(repository instanceof MockSosRepository);
    }

    @Test
    public void createRepositoryForMode_returnsApiInLiveMode() {
        SosRepository repository = RepositoryProvider.createRepositoryForMode(false);
        assertTrue(repository instanceof ApiSosRepository);
    }
}
