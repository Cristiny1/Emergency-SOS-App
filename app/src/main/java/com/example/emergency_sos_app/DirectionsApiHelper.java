package com.example.emergency_sos_app;

import android.graphics.Color;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Dash;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.Arrays;
import java.util.List;

public class DirectionsApiHelper {

    public static Polyline drawRoute(GoogleMap map, LatLng start, LatLng end) {
        List<PatternItem> pattern = Arrays.asList(new Dash(30), new Gap(20));
        
        Polyline line = map.addPolyline(new PolylineOptions()
                .add(start, end)
                .width(14)
                .color(Color.parseColor("#1877F2")) // Primary SOS Blue
                .pattern(pattern)
                .geodesic(true));
        
        line.setTag("animated");
        return line;
    }
}