package com.example.ikerlopez.trackme;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import android.widget.EditText;
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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MenuActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback, RecyclerViewAdapter.ItemClickListener {

    GoogleMap mMap;
    RecyclerView recyclerView;
    RecyclerViewAdapter recyclerViewAdapter;
    FloatingActionButton fab;

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
    ValueEventListener portaRutas;

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    int index = 0;
    String idruta = null;
    boolean traking = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        setTitle("TrackMe");

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!traking)
                    createRuta();
                else
                    finishRoute();
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

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mSettingsClient = LocationServices.getSettingsClient(this);

        createLocationCallback();
        createLocationRequest();
        buildLocationSettingsRequest();

        portaRutas = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                LatLng previousLocation = null;
                LatLng nextLocation;
                for(DataSnapshot postSnapshot: dataSnapshot.getChildren()) {
                    UserLocation userLocation = postSnapshot.getValue(UserLocation.class);
                    double lat = userLocation.getLatitud();
                    double lng = userLocation.getLongitude();
                    nextLocation = new LatLng(lat,lng);
                    if(previousLocation == null){
                        previousLocation = nextLocation;
                        mMap.moveCamera(CameraUpdateFactory.newLatLng(nextLocation));
                        setRouteMarker(previousLocation,true);
                    }

                    drawLine(previousLocation,nextLocation);
                    previousLocation = nextLocation;
                }
                setRouteMarker(previousLocation,false);
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(getApplicationContext(),"Error al traer ruta",Toast.LENGTH_SHORT).show();
                Log.e("TRAER RUTA",databaseError.getMessage());
            }
        };


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
        int id = item.getItemId();

        if (id == R.id.nav_routes) {
            if (recyclerView.getVisibility() == View.VISIBLE) {
                recyclerView.setVisibility(View.INVISIBLE);
            } else {
                getPreviousRoutes();
            }
        } else if (id == R.id.nav_settings) {
            Toast.makeText(this,"Pendiente de implementar",Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_notifications) {
            Toast.makeText(this,"Pendiente de implementar",Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_heat_map) {
            Toast.makeText(this,"Pendiente de implementar",Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_clear) {
            mMap.clear();
            Toast.makeText(this,"Mapa limpiado",Toast.LENGTH_SHORT).show();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void finishRoute(){
        mFusedLocationClient.removeLocationUpdates(mLocationCallback)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Toast.makeText(getApplicationContext(),"Fin de ruta",Toast.LENGTH_SHORT).show();
                        updateRouteButton();
                        LatLng latLng = new LatLng(lastLocation.getLatitude(),lastLocation.getLongitude());
                        setRouteMarker(latLng,false);
                    }
                });
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


    private void createRuta(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Nueva ruta");

        final EditText input = new EditText(this);
        input.setHint("Nombre de nueva ruta");
        builder.setView(input);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String nombre = input.getText().toString();
                startLocationUpdates(nombre);
            }
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }


    private void createLocationCallback() {
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                Location mCurrentLocation = locationResult.getLastLocation();
                addLocationToFirebase(mCurrentLocation);
                LatLng latLng1 = new LatLng(mCurrentLocation.getLatitude(),mCurrentLocation.getLongitude());
                if(lastLocation == null) {
                    lastLocation = mCurrentLocation;
                    setRouteMarker(latLng1,true);
                }
                LatLng latLng2 = new LatLng(lastLocation.getLatitude(),lastLocation.getLongitude());
                drawLine(latLng1,latLng2);
                lastLocation = mCurrentLocation;
            }
        };
    }

    private void setRouteMarker(LatLng location,boolean inicio){
        MarkerOptions marker = new MarkerOptions();
        marker.position(location);
        if (inicio){
            marker.title("Inicio de ruta");
            marker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
        } else
            marker.title("Fin de ruta");
        mMap.addMarker(marker);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(location));
    }

    private void addLocationToFirebase(Location location){
        index++;
        UserLocation userLocation = new UserLocation();
        userLocation.setIdruta(idruta);
        userLocation.setIndex(index);
        userLocation.setLatitud(location.getLatitude());
        userLocation.setLongitude(location.getLongitude());
        userLocation.setTime(DateFormat.getDateTimeInstance().format(new Date()));
        puntosRef.push().setValue(userLocation);
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

    private void startLocationUpdates(final String name) {
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
                        Ruta ruta = new Ruta(idruta,name);
                        Toast.makeText(getApplicationContext(),"Traking iniciado",Toast.LENGTH_SHORT).show();
                        updateRouteButton();
                        rutasManager.addFavorite(getApplicationContext(),ruta);
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
        puntosRef.orderByChild("idruta").equalTo(rutas.get(position).getId()).addListenerForSingleValueEvent(portaRutas);
    }

    public void updateRouteButton(){
        if(traking) {
            fab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_green_dark)));
            fab.setImageResource(R.drawable.ic_run);
            traking = false;
        } else {
            fab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_red_dark)));
            fab.setImageResource(R.drawable.ic_human_male);
            traking = true;
        }
    }

}
