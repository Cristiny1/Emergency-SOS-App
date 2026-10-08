package com.example.emergency_sos_app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import androidx.core.app.NotificationCompat;

public class SosAlertManager {

    private static final String TAG = "SosAlertManager";
    private static final String CHANNEL_USER = "sos_user_channel";
    private static final String CHANNEL_CENTER = "sos_center_channel";

    private final Context context;
    private Ringtone siren;
    private Vibrator vibrator;

    public SosAlertManager(Context context) {
        this.context = context;
        // FIX: getSystemService / getDefaultVibrator can return null on some
        // devices or emulators. Wrap in try/catch and null-check so a missing
        // vibration service doesn't crash the app on construction.
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                android.os.VibratorManager vibratorManager =
                        (android.os.VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                this.vibrator = (vibratorManager != null) ? vibratorManager.getDefaultVibrator() : null;
            } else {
                this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            }
        } catch (Exception e) {
            Log.e(TAG, "Vibrator unavailable: " + e.getMessage());
            this.vibrator = null;
        }
        createNotificationChannels();
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = context.getSystemService(NotificationManager.class);
            // FIX: getSystemService can return null; guard before using it.
            if (nm == null) return;

            NotificationChannel userChannel = new NotificationChannel(
                    CHANNEL_USER, "SOS Status", NotificationManager.IMPORTANCE_HIGH);

            NotificationChannel centerChannel = new NotificationChannel(
                    CHANNEL_CENTER, "SOS Center Alerts", NotificationManager.IMPORTANCE_HIGH);

            nm.createNotificationChannel(userChannel);
            nm.createNotificationChannel(centerChannel);
        }
    }

    public void startSiren() {
        startSiren(false);
    }

    public void startSiren(boolean isSilent) {
        if (!isSilent) {
            try {
                if (siren == null) {
                    Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                    siren = RingtoneManager.getRingtone(context, notification);
                }
                if (siren != null) {
                    try {
                        if (!siren.isPlaying()) siren.play();
                    } catch (Exception ignored) {
                    }
                } else {
                    Log.w(TAG, "No siren ringtone available on this device.");
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to play siren: " + e.getMessage());
            }
        }

        // Always vibrate for tactile feedback unless it's a deep-stealth mode
        if (vibrator != null) {
            long[] pattern = {0, 500, 200, 500};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, isSilent ? -1 : 0));
            } else {
                vibrator.vibrate(pattern, isSilent ? -1 : 0);
            }
        }
    }

    public void stopSiren() {
        if (siren != null && siren.isPlaying()) {
            siren.stop();
        }
        if (vibrator != null) {
            vibrator.cancel();
        }
    }

    public void showUserStatusNotification(String status) {
        if (!context.getSharedPreferences("sos_profile_prefs", Context.MODE_PRIVATE)
                .getBoolean("notifications_enabled", true)) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_USER)
                .setSmallIcon(R.drawable.ic_sos_shield)
                .setContentTitle("Emergency SOS Active")
                .setContentText("Status: " + status)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true);

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        // FIX: null-check before calling notify().
        if (nm != null) {
            nm.notify(101, builder.build());
        }
    }

    public void showCenterAlert(String userName, String type) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        Intent intent = new Intent(context, SosCenterActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_CENTER)
                .setSmallIcon(R.drawable.ic_sos_shield)
                .setContentTitle("SOS CENTER: INCOMING ALERT")
                .setContentText(type + " emergency from " + userName)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        // FIX: null-check before calling notify().
        if (nm != null) {
            nm.notify(202, builder.build());
        }
    }

    public void clearNotifications() {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        // FIX: null-check before calling cancel().
        if (nm != null) {
            nm.cancel(101);
            nm.cancel(202);
        }
    }
}
