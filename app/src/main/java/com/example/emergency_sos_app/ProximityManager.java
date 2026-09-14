package com.example.emergency_sos_app;

import android.location.Location;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages proximity calculations and finding the nearest emergency facilities.
 */
public class ProximityManager {

    public static class Facility {
        public String name, category, phone;
        public double lat, lng;
        public float distanceKm;
        public int travelTimeMins;

        public Facility(String name, String category, String phone, double lat, double lng) {
            this.name = name;
            this.category = category;
            this.phone = phone;
            this.lat = lat;
            this.lng = lng;
        }
    }

    private static final List<Facility> facilities = new ArrayList<>();

    static {
        facilities.add(new Facility("Siem Reap Provincial Police", "POLICE", "117", 13.3633, 103.8567));
        facilities.add(new Facility("Siem Reap Fire Station", "FIRE", "118", 13.3556, 103.8544));
        facilities.add(new Facility("Siem Reap Referral Hospital", "MEDICAL", "063761111", 13.3622, 103.8599));
        facilities.add(new Facility("Angkor Hospital for Children", "MEDICAL", "063963409", 13.3639, 103.8547));
        facilities.add(new Facility("Jayavarman VII Hospital", "MEDICAL", "063963409", 13.3761, 103.8592));
    }

    public static List<Facility> getNearestFacilities(double userLat, double userLng, String categoryFilter) {
        List<Facility> results = new ArrayList<>();
        for (Facility f : facilities) {
            if (categoryFilter == null || f.category.equalsIgnoreCase(categoryFilter)) {
                float[] dist = new float[1];
                Location.distanceBetween(userLat, userLng, f.lat, f.lng, dist);
                f.distanceKm = dist[0] / 1000f;
                // Refined for City Density (3 min/km)
                f.travelTimeMins = (int) (f.distanceKm * 3.0);
                results.add(f);
            }
        }
        Collections.sort(results, (f1, f2) -> Float.compare(f1.distanceKm, f2.distanceKm));
        return results;
    }

    public static Facility getNearest(double userLat, double userLng, String category) {
        List<Facility> nearest = getNearestFacilities(userLat, userLng, category);
        return nearest.isEmpty() ? null : nearest.get(0);
    }
}
