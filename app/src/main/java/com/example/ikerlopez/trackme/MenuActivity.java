package com.example.ikerlopez.trackme;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MenuActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback, RecyclerViewAdapter.ItemClickListener {

    GoogleMap mMap;
    RecyclerView recyclerView;
    RecyclerViewAdapter recyclerViewAdapter;

    private ArrayList<Ruta> rutas;
    private RutasManager rutasManager;

    FusedLocationProviderClient mFusedLocationClient;
    SettingsClient mSettingsClient;
    Location lastLocation;
    LocationCallback mLocationCallback;
    LocationRequest mLocationRequest;
    LocationSettingsRequest mLocationSettingsRequest;

    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference rutasRef = database.getReference("rutas");
    DatabaseReference puntosRef = database.getReference("puntos");

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    int index = 0;
    String idruta = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        recyclerView = (RecyclerView) findViewById(R.id.recyclerview);
        LinearLayoutManager verticalManager = new LinearLayoutManager(MenuActivity.this, LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(verticalManager);
        rutasManager = new RutasManager();
        mockRutas();

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mSettingsClient = LocationServices.getSettingsClient(this);

        createLocationCallback();
        createLocationRequest();
        buildLocationSettingsRequest();

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            startLocationUpdates();
        } else if (id == R.id.nav_gallery) {

            ValueEventListener rutas = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for(DataSnapshot postSnapshot: dataSnapshot.getChildren()) {
                        UserLocation userLocation = postSnapshot.getValue(UserLocation.class);
                        Log.d("PUNTOS", userLocation.getIdruta());
                        //Log.d("PUNTOS", userLocation.getIndex());
                        Log.d("PUNTOS", userLocation.getLatitud());
                        Log.d("PUNTOS", userLocation.getLongitude());
                        Log.d("PUNTOS", userLocation.getTime());
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            };
            puntosRef.orderByChild("idruta").equalTo("-LDNgo-oMOH5I3Vs33R8").addListenerForSingleValueEvent(rutas);

        } else if (id == R.id.nav_slideshow) {
            if (recyclerView.getVisibility() == View.VISIBLE) {
                recyclerView.setVisibility(View.INVISIBLE);
            } else {
                getPreviousRoutes();
            }
            //mockRutas();
        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {
            mMap.clear();
        } else if (id == R.id.nav_send) {
            LatLng madrid = new LatLng(40, -4);
            LatLng madagascar = new LatLng(-20.65, 46.131);
            mMap.addMarker(new MarkerOptions().position(madrid).title("Madrid"));
            mMap.addMarker(new MarkerOptions().position(madagascar).title("Madagascar"));
            drawLine(madrid,madagascar);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void getPreviousRoutes () {
        /*
            Carga rutas anteriores guardadas en sharedPreferences y las muestra en RecyclerView para que el usuario seleccione la que desea visualizar
         */
        rutas = rutasManager.getFavorites(getApplicationContext());
        if (rutas != null) {
            // Pued ser que no haya rutas previas guardadas
            recyclerViewAdapter = new RecyclerViewAdapter(getApplicationContext(), rutas);
            recyclerViewAdapter.setClickListener(this);
            recyclerView.setAdapter(recyclerViewAdapter);
            recyclerView.setVisibility(View.VISIBLE);
        }

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            checkLocationPermission();
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            //return;
        }
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney").icon(BitmapDescriptorFactory.defaultMarker(
                BitmapDescriptorFactory.HUE_AZURE)));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle("Location Permission Needed")
                        .setMessage("This app needs the Location permission, please accept to use location functionality")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(getParent(),
                                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION );
                            }
                        })
                        .create()
                        .show();


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION );
            }
        }
    }

    private void mockRutas () {
        /*
            Función para mockear una serie de rutas como si las hubiera realizado el usuario y estuvieran almacenadas en Shared Preferences
            Llamarle al menos una vez para que el
         */
        Ruta ruta = new Ruta("-LDNgo-oMOH5I3Vs33R8","Primera Ruta");
        Ruta ruta2 = new Ruta("-LDQd9Xlt_M0zhGJTybv","Segunda Ruta");
        Ruta ruta3 = new Ruta("-LDOMShrXjcO-eL-Elip","Tercera Ruta");

        rutasManager.addFavorite(getApplicationContext(), ruta);
        rutasManager.addFavorite(getApplicationContext(), ruta3);
        rutasManager.addFavorite(getApplicationContext(), ruta2);

    }

    private void createLocationCallback() {
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                Location mCurrentLocation = locationResult.getLastLocation();
                Log.d("LOCATION", String.format(Locale.ENGLISH, "%s: %f", "Latitud: ",
                        mCurrentLocation.getLatitude()));
                Log.d("LOCATION", String.format(Locale.ENGLISH, "%s: %f", "Longitud: ",
                        mCurrentLocation.getLongitude()));
                String mLastUpdateTime = DateFormat.getDateTimeInstance().format(new Date());
                Log.d("LOCATION", String.format(Locale.ENGLISH, "%s: %s", "Date: ", mLastUpdateTime));

                index++;
                UserLocation userLocation = new UserLocation();
                userLocation.setIdruta(idruta);
                userLocation.setIndex(index);
                userLocation.setLatitud(String.format(Locale.getDefault(),"%f",mCurrentLocation.getLatitude()));
                userLocation.setLongitude(String.format(Locale.getDefault(),"%f",mCurrentLocation.getLongitude()));
                userLocation.setTime(DateFormat.getDateTimeInstance().format(new Date()));
                puntosRef.push().setValue(userLocation);

                if(lastLocation == null)
                    lastLocation = mCurrentLocation;
                LatLng latLng1 = new LatLng(mCurrentLocation.getLatitude(),mCurrentLocation.getLongitude());
                LatLng latLng2 = new LatLng(lastLocation.getLatitude(),lastLocation.getLongitude());
                drawLine(latLng1,latLng2);
                lastLocation = mCurrentLocation;
            }
        };
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();
    }

    private void startLocationUpdates() {
        mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
                .addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        Log.i("LOCATION", "All location settings are satisfied.");
                        if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                                ActivityCompat.checkSelfPermission(getApplicationContext(),
                                        android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            // TODO: Consider calling
                            //    ActivityCompat#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for ActivityCompat#requestPermissions for more details.
                            return;
                        }
                        idruta = rutasRef.push().getKey();
                        index = 0;
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                                mLocationCallback, Looper.myLooper());

                    }
                });
    }

    public void drawLine(LatLng point1, LatLng point2){
        PolylineOptions lineOptions = new PolylineOptions();
        lineOptions.add(point1);
        lineOptions.add(point2);

        lineOptions.width(5);
        lineOptions.color(Color.BLUE);
        lineOptions.geodesic(true);

        mMap.addPolyline(lineOptions);
    }


    @Override
    public void onItemClick(View view, int position) {
        // Gestionar la pulsación del recyclerview para traer de la base de datos la ruta seleccionada y pintarla sobre el mapa

        Toast.makeText(this, "Has seleccionado la ruta " + rutas.get(position).getNombre() , Toast.LENGTH_SHORT).show();


    }
}
