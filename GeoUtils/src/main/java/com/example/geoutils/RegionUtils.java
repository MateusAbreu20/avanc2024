package com.example.geoutils;

import com.google.android.gms.maps.model.LatLng;
import java.util.Queue;

public class RegionUtils {

    // Função para verificar se a nova região está dentro de 5 km de qualquer região existente
    public static boolean isWithinRadius(LatLng newRegionLocation, Queue<Region> regionQueue) {
        final double MAX_DISTANCE_METERS = 5000; // 5 km em metros

        for (Region existingRegion : regionQueue) {
            LatLng existingRegionLocation = new LatLng(existingRegion.getLatitude(), existingRegion.getLongitude());
            double distance = distanceBetweenLocations(newRegionLocation, existingRegionLocation);
            if (distance < MAX_DISTANCE_METERS) {
                return true; // Retorna verdadeiro se estiver dentro do raio de 5 km
            }
        }
        return false; // Retorna falso se não estiver dentro do raio de 5 km de nenhuma região existente
    }

    // Função auxiliar para calcular a distância entre duas coordenadas geográficas
    public static double distanceBetweenLocations(LatLng location1, LatLng location2) {
        double earthRadius = 6371000; // Raio da Terra em metros
        double lat1 = Math.toRadians(location1.latitude);
        double lon1 = Math.toRadians(location1.longitude);
        double lat2 = Math.toRadians(location2.latitude);
        double lon2 = Math.toRadians(location2.longitude);
        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1) * Math.cos(lat2) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }
}
