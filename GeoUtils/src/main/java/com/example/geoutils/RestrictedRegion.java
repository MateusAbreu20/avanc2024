package com.example.geoutils;

public class RestrictedRegion extends Region {
    private Region mainRegion;

    public RestrictedRegion(String name, double latitude, double longitude, int user, long timestamp, boolean restricted) {
        super(name, latitude, longitude, user, timestamp);
    }

    public Region getMainRegion() {
        return mainRegion;
    }

    public void setMainRegion(Region mainRegion) {
        this.mainRegion = mainRegion;
    }
}
