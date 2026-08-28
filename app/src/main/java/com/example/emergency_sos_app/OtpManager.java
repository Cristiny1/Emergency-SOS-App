package com.example.emergency_sos_app;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import java.util.Random;

/**
 * Manages OTP generation and verification for both Email and SMS.
 * Currently operates in Simulation Mode for developer testing.
 */
public class OtpManager {

    public enum DeliveryMethod { SMS, EMAIL }

    private static OtpManager instance;
    private String currentOtp;
    private String lastDestination;
    private DeliveryMethod lastMethod;

    private OtpManager() {}

    public static synchronized OtpManager getInstance() {
        if (instance == null) {
            instance = new OtpManager();
        }
        return instance;
    }

    /**
     * Simulates sending an OTP to the given destination (phone or email).
     */
    public void sendOtp(Context context, String destination, DeliveryMethod method) {
        this.lastDestination = destination;
        this.lastMethod = method;
        
        // Generate a random 6-digit code
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        this.currentOtp = String.valueOf(code);

        // Simulation: Show the code in a Toast
        String prefix = (method == DeliveryMethod.SMS) ? "SMS Sent to " : "Email Sent to ";
        
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Toast.makeText(context, 
                "SIMULATION: " + prefix + destination + "\nCode: " + currentOtp, 
                Toast.LENGTH_LONG).show();
        }, 1000);
    }

    public boolean verifyOtp(String code) {
        return currentOtp != null && currentOtp.equals(code);
    }

    public String getLastDestination() {
        return lastDestination;
    }

    public DeliveryMethod getLastMethod() {
        return lastMethod;
    }
}
