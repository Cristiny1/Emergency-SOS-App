package com.example.emergency_sos_app;

import android.util.Log;

import java.net.URISyntaxException;
import java.util.Collections;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class SocketManager {
    private static final String TAG = "SocketManager";
    private static final String SERVER_URL = "http://10.0.2.2:5000";

    private static SocketManager instance;
    private Socket socket;
    private String currentToken;

    private SocketManager() {}

    public static synchronized SocketManager getInstance() {
        if (instance == null) {
            instance = new SocketManager();
        }
        return instance;
    }

    public void connect(String token) {
        if (socket != null && socket.connected() && 
            ((token == null && currentToken == null) || (token != null && token.equals(currentToken)))) {
            return;
        }

        if (socket != null) {
            socket.disconnect();
            socket.off();
        }

        currentToken = token;
        try {
            IO.Options options = IO.Options.builder()
                    .setReconnection(true)
                    .setAuth(token != null ? Collections.singletonMap("token", token) : null)
                    .build();
            socket = IO.socket(SERVER_URL, options);
            socket.connect();
        } catch (URISyntaxException e) {
            Log.e(TAG, "Socket connection failed: " + e.getMessage());
        }
    }

    public void disconnect() {
        if (socket != null) {
            socket.disconnect();
        }
    }

    public boolean isConnected() {
        return socket != null && socket.connected();
    }

    public void emit(String event, Object... args) {
        if (socket != null && socket.connected()) {
            socket.emit(event, args);
        }
    }

    public void on(String event, Emitter.Listener listener) {
        if (socket != null) {
            socket.on(event, listener);
        }
    }

    public void off(String event) {
        if (socket != null) {
            socket.off(event);
        }
    }
}