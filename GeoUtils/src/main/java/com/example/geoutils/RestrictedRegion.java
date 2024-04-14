package com.example.geoutils;

public class RestrictedRegion extends Region {
    private boolean restricted;

    public RestrictedRegion(String name, double latitude, double longitude, int user, long timestamp, boolean restricted) {
        super(name, latitude, longitude, user, timestamp);
        this.restricted = restricted;
    }

    public boolean isRestricted() {
        return restricted;
    }

    public void setRestricted(boolean restricted) {
        this.restricted = restricted;
    }
}
