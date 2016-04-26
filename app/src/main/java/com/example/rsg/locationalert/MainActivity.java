package com.example.rsg.locationalert;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private Button find_;
    private EditText enterLocation_;
    private GoogleMap mMap;
    private static String preference = "PREFS";
    protected GoogleApiClient mGoogleApiClient;
    protected Location mCurrentLocation;
    protected LatLng lastLoc;
    protected TextView mLatLngText;
    protected SharedPreferences lastLocation;
    protected int PLACE_PICKER_REQUEST = 1;
    private Place place;
    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;

    /**
     * The fastest rate for active location updates. Exact. Updates will never be more frequent
     * than this value.
     */
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        find_ = (Button) findViewById(R.id.buttonFind);

        find_.setOnClickListener(this);

        mLatLngText = (TextView) findViewById(R.id.textLatLng);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        lastLocation = getSharedPreferences(preference, Context.MODE_PRIVATE);

        buildGoogleApiClient();

    }


    /**
     * Builds a GoogleApiClient. Uses the {@code #addApi} method to request the
     * LocationServices API.
     */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(this, this)
                .build();
    }

    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Within {@code onPause()}, we pause location updates, but leave the
        // connection to GoogleApiClient intact.  Here, we resume receiving
        // location updates if the user has requested them.

        double lat = Double.longBitsToDouble(lastLocation.getLong("Lat", 0));
        double lng = Double.longBitsToDouble(lastLocation.getLong("Lng", 0));

        if (lat != 0 && lng != 0){
            lastLoc = new LatLng(lat, lng);

            if (mMap != null) {
                mMap.addMarker(new MarkerOptions().position(lastLoc));
                mMap.moveCamera(CameraUpdateFactory.newLatLng(lastLoc));
            }
        }

        //Log.d("debug", "Lat: " + lat + " Long: " + lng);

    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mCurrentLocation != null) {
            SharedPreferences.Editor edit = lastLocation.edit();
            edit.putLong("Lat", Double.doubleToRawLongBits(mCurrentLocation.getLatitude()));
            edit.putLong("Lng", Double.doubleToRawLongBits(mCurrentLocation.getLongitude()));
            edit.commit();
        }

        //SharedPreferences set1 = getSharedPreferences(preference, Context.MODE_PRIVATE);

    }



    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case (R.id.buttonFind):
                PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();

                try {
                    startActivityForResult(builder.build(this), PLACE_PICKER_REQUEST);
                } catch (GooglePlayServicesRepairableException e) {

                    Log.d("Place picker: " , e.toString());
                } catch (GooglePlayServicesNotAvailableException e) {
                    Log.d("Place picker: " , e.toString());
                }
                break;
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == RESULT_OK) {
                //set the new place
                place = PlacePicker.getPlace(this, data);
                String toastMsg = String.format("Place: %s", place.getName());
                Toast.makeText(this, toastMsg, Toast.LENGTH_LONG).show();
                //add a marker at the location of the new place
                LatLng requestedPlace = place.getLatLng();
                mMap.addMarker(new MarkerOptions().position(requestedPlace));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(requestedPlace, 12.0f));

                Log.d("requested place", "Lat: " + requestedPlace.latitude + " Long: " + requestedPlace.longitude);


                LatLng placeLoc = place.getLatLng();
                float[] results = new float[1];

                //calculate the distance and store it in results
                Location.distanceBetween(placeLoc.latitude, placeLoc.longitude,
                        mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude(), results);

                //make a toast whenever the distance is <= 200
                int distance = (int) results[0];
                if(distance <= 1000) {
                    toastMsg = String.format("Within 200meters of %s", place.getName());
                    Toast.makeText(this, toastMsg, Toast.LENGTH_LONG).show();
                }


            }
        }
    }



    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
/*        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));*/

        if (lastLoc != null) {
            //add a marker to the current position
            mMap.addMarker(new MarkerOptions().position(lastLoc));

            //zoom in and reposition the camera to the marker at the same time
            //mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(lastLoc, 12.0f));
        }

    }

    @Override
    public void onConnected(Bundle connectionHint) {

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);

            return;
        }

        Log.d("debug", "connected");

        mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        double lat;
        double lng;

        if (mCurrentLocation != null) {
            lat = mCurrentLocation.getLatitude();
            lng = mCurrentLocation.getLongitude();
//            LatLng currentLoc = new LatLng(lat, lng);
//            mMap.addMarker(new MarkerOptions().position(currentLoc));
//            mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLoc));

            mLatLngText.setText("Lat: " + lat + "\nLng: " + lng);
        }

    }


    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

}
