package com.example.emergency_sos_app;

import android.content.Context;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.MapStyleOptions;

import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.MapStyleOptions;

public class GoogleMapHelper {

    public static void applySilverStyle(Context context, GoogleMap map) {
        try {
            map.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style));
        } catch (Exception ignored) {}
    }

    public static MarkerOptions getUserMarkerOptions(LatLng position) {
        return new MarkerOptions()
                .position(position)
                .title("My Location")
                .anchor(0.5f, 0.5f)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
    }

    public static MarkerOptions getResponderMarkerOptions(LatLng position) {
        return new MarkerOptions()
                .position(position)
                .title("Rescue Unit")
                .flat(true)
                .anchor(0.5f, 0.5f)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_responder_car));
    }

    public static CameraPosition get3DCameraPosition(LatLng target, float zoom, float bearing) {
        return new CameraPosition.Builder()
                .target(target)
                .zoom(zoom)
                .tilt(60f) // Professional 3D Tilt
                .bearing(bearing)
                .build();
    }

    public static CameraPosition getChaseCameraPosition(LatLng target, float bearing, float distance) {
        // As responder gets closer, we zoom in more and increase tilt
        float zoom = 15f;
        if (distance < 2000) zoom = 16f;
        if (distance < 1000) zoom = 17f;
        if (distance < 500) zoom = 18f;

        return new CameraPosition.Builder()
                .target(target)
                .zoom(zoom)
                .tilt(55f)
                .bearing(bearing) // Face the direction of the car
                .build();
    }
}