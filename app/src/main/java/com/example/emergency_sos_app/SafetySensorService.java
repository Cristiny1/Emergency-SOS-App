package com.example.emergency_sos_app;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.util.Log;

public class SafetySensorService extends Service implements SensorEventListener {

    private static final String TAG = "SafetySensor";
    private SensorManager sensorManager;
    
    // Threshold for crash detection (G-force spike)
    private static final float CRASH_THRESHOLD = 35.0f; // Approx 3.5g
    private long lastTriggerTime = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (accelerometer != null) {
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
                Log.d(TAG, "Accelerometer monitoring started");
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            double gForce = Math.sqrt(x * x + y * y + z * z);
            if (gForce > CRASH_THRESHOLD) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastTriggerTime > 5000) { // 5s debounce
                    lastTriggerTime = currentTime;
                    Log.w(TAG, "Potential Impact Detected! G-Force: " + gForce);
                    triggerImpactAlert();
                }
            }
        }
    }

    private void triggerImpactAlert() {
        Intent intent = new Intent(this, DashboardActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("IMPACT_DETECTED", true);
        startActivity(intent);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
