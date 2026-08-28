package com.example.emergency_sos_app;

import android.os.Handler;
import android.os.SystemClock;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

public class MapAnimation {

    public interface AnimationCallback {
        void onUpdate(LatLng latLng);
    }

    public static void animateMarker(Marker marker, LatLng destination, AnimationCallback callback) {
        final LatLng startPosition = marker.getPosition();
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        final Interpolator interpolator = new LinearInterpolator();
        final float duration = 1500;

        marker.setRotation((float) calculateBearing(startPosition, destination));

        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed / duration);
                double lng = t * destination.longitude + (1 - t) * startPosition.longitude;
                double lat = t * destination.latitude + (1 - t) * startPosition.latitude;
                LatLng intermediate = new LatLng(lat, lng);
                
                marker.setPosition(intermediate);
                if (callback != null) callback.onUpdate(intermediate);

                if (t < 1.0) {
                    handler.postDelayed(this, 16);
                }
            }
        });
    }

    public static double calculateBearing(LatLng start, LatLng end) {
        double lat1 = Math.toRadians(start.latitude);
        double lng1 = Math.toRadians(start.longitude);
        double lat2 = Math.toRadians(end.latitude);
        double lng2 = Math.toRadians(end.longitude);

        double dLon = (lng2 - lng1);
        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);
        return Math.toDegrees(Math.atan2(y, x));
    }
}