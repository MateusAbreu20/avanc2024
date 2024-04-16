package com.example.geoutils;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import java.io.Serializable;

public class Region implements Serializable {
    private String name;
    private double latitude;
    private double longitude;
    private int user;
    private long timestamp;

    private Marker marker; // Adicione este atributo

    public void setMarker(Marker marker) {
        this.marker = marker;
    }

    // Construtor vazio
    // Construtor com parâmetros
    public Region(String name, double latitude, double longitude, int user, long timestamp) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.user = user;
        this.timestamp = timestamp;
    }

    // Getters e Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public int getUser() {
        return user;
    }

    public void setUser(int user) {
        this.user = user;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    // Método para calcular a distância entre duas coordenadas geográficas
    public double distanceBetweenLocations(LatLng location1, LatLng location2) {
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

    // Método para obter o marcador associado à região
    public Marker getMarker() {
        return this.marker;
    }
}
