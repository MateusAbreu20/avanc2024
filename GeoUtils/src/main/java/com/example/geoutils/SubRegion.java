package com.example.geoutils;

public class SubRegion extends Region {
    private Region mainRegion;

    public SubRegion(String name, double latitude, double longitude, int user, long timestamp, Region mainRegion) {
        super(name, latitude, longitude, user, timestamp);
        this.mainRegion = mainRegion;
    }

    public Region getMainRegion() {
        return mainRegion;
    }

    public void setMainRegion(Region mainRegion) {
        this.mainRegion = mainRegion;
    }

    // Método para obter a região principal (parent region) da sub-região
    public Region getParentRegion() {
        return mainRegion;
    }

}
