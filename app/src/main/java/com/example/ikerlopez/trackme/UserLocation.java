package com.example.ikerlopez.trackme;


/**
 * Created by Delme on 25/05/2018.
 */

public class UserLocation {
    private String idruta;
    private int index;
    private String latitud;
    private String longitude;
    private String time;

    public UserLocation(){
    }

    public String getIdruta() {
        return idruta;
    }

    public void setIdruta(String idruta) {
        this.idruta = idruta;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getLatitud() {
        return latitud;
    }

    public void setLatitud(String latitud) {
        this.latitud = latitud;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}
