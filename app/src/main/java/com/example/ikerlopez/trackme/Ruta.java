package com.example.ikerlopez.trackme;

public class Ruta {

    private String id;
    private String nombre;

    Ruta(String id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }

    public String getId() {
        return id;
    }
}
