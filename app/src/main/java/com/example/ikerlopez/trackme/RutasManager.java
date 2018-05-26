package com.example.ikerlopez.trackme;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RutasManager {

    /*
        Clase para la gestión del almacenamiento interno de rutas. Permite almacenar y recuperar rutas de SharedPreferences
     */
    public static final String PREFS_NAME = "RUTAS";
    public static final String FAVORITES = "RUTAS_FAVORITES";

    public RutasManager() {
        super();
    }

    // This four methods are used for maintaining favorites.
    public void saveFavorites(Context context, ArrayList<Ruta> favorites) {
        SharedPreferences settings;
        SharedPreferences.Editor editor;

        settings = context.getSharedPreferences(PREFS_NAME,
                Context.MODE_PRIVATE);
        editor = settings.edit();

        Gson gson = new Gson();
        String jsonFavorites = gson.toJson(favorites);

        editor.putString(FAVORITES, jsonFavorites);

        editor.commit();
    }

    public void addFavorite(Context context, Ruta ruta) {
        ArrayList<Ruta> favorites = getFavorites(context);
        if (favorites == null)
            favorites = new ArrayList<Ruta>();
        favorites.add(ruta);
        saveFavorites(context, favorites);
    }

    public void removeFavorite(Context context, Ruta ruta) {
        ArrayList<Ruta> favorites = getFavorites(context);
        if (favorites != null) {
            favorites.remove(ruta);
            saveFavorites(context, favorites);
        }
    }

    public ArrayList<Ruta> getFavorites(Context context) {
        SharedPreferences settings;
        List<Ruta> favorites;

        settings = context.getSharedPreferences(PREFS_NAME,
                Context.MODE_PRIVATE);

        if (settings.contains(FAVORITES)) {
            String jsonFavorites = settings.getString(FAVORITES, null);
            Gson gson = new Gson();
            Ruta[] favoriteItems = gson.fromJson(jsonFavorites,
                    Ruta[].class);

            favorites = Arrays.asList(favoriteItems);
            favorites = new ArrayList<Ruta>(favorites);
        } else
            return null;

        return (ArrayList<Ruta>) favorites;
    }
}
