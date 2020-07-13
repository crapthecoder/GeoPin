package com.luckweaver.geopin;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.os.*;
import android.location.*;
import android.util.*;
import android.widget.*;

import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.android.gms.maps.GoogleMap.*;

import java.text.*;
import java.util.*;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, OnMapLongClickListener, OnInfoWindowClickListener {

    private GoogleMap mMap;

    LocationManager locationManager;
    LocationListener locationListener;

    Marker lastMarker = null;
    Location lastLocation = null;

    SharedPreferences preferences;

    public void addMarker(LatLng latLng, String text) {
        mMap.addMarker(new MarkerOptions().position(latLng).title(text));
    }

    public Marker addMarker(LatLng latLng, String text, float hue) {
        return mMap.addMarker(new MarkerOptions().position(latLng).title(text).icon(BitmapDescriptorFactory.defaultMarker(hue)));
    }

    public void centerAndZoom(LatLng latLng, int zoom) {
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, zoom);
        mMap.animateCamera(cameraUpdate);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) {
            if (grantResults.length > 0) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    startLocationListener();
                else
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }
    }

    boolean showed = false;
    int show_index = -1;

    public void startLocationListener() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }

    Geocoder geocoder;

    public List<Address> getAddressList(LatLng latLng) {
        try {
            return geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getAddress(LatLng latLng) {
        List<Address> addresses = getAddressList(latLng);

        if (addresses == null || addresses.size() == 0 || addresses.get(0).getAddressLine(0) == null)
            return "Address not found";

        return addresses.get(0).getAddressLine(0);
    }

    public void initialize(SharedPreferences preferences) {
        if (!preferences.getAll().containsKey("locations"))
            preferences.edit().putString("locations", "").apply();

        if (!Objects.equals(preferences.getString("locations", ""), "")) {
            String[] savedLocations = Objects.requireNonNull(preferences.getString("locations", "")).trim().split(" ");

            if (savedLocations.length != MainActivity.latLngList.size() * 2) {
                for (int i = 0; i < savedLocations.length; i += 2) {
                    Log.i("dummy tag", savedLocations[i]);
                    LatLng newLatLng = new LatLng(Double.parseDouble(savedLocations[i]), Double.parseDouble(savedLocations[i + 1]));

                    MainActivity.latLngList.add(newLatLng);
                    MainActivity.listElements.add(format(newLatLng));
                    MainActivity.arrayAdapter.notifyDataSetChanged();
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        preferences = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
        geocoder = new Geocoder(this, Locale.getDefault());

        show_index = getIntent().getIntExtra("show_index", -1);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapLongClickListener(this);
        mMap.setOnInfoWindowClickListener(this);

        initialize(preferences);

        for (int i = 0; i < MainActivity.latLngList.size(); i++) {
            LatLng latLng = MainActivity.latLngList.get(i);
            addMarker(latLng, MainActivity.listElements.get(i));

            if (i == show_index) {
                centerAndZoom(latLng, 10);
                showed = true;
            }
        }

        show_index = -1;

        locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                if (lastLocation == null || !(location.getLatitude() == lastLocation.getLatitude() && location.getLongitude() == lastLocation.getLongitude())) {
                    if (lastMarker != null)
                        lastMarker.remove();

                    LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    String address = getAddress(currentLocation);

                    if (!address.equals("Address not found"))
                        lastMarker = addMarker(currentLocation, "Your current location: " + address, BitmapDescriptorFactory.HUE_BLUE);
                    else
                        lastMarker = addMarker(currentLocation, "Could not find an address for your current location", BitmapDescriptorFactory.HUE_BLUE);

                    if (lastLocation == null) {
                        if (!showed)
                            centerAndZoom(currentLocation, 10);
                        else
                            showed = false;
                    }

                    lastLocation = location;
                }
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderDisabled(@NonNull String provider) {
                askToEnableGPS();
            }

            @Override
            public void onProviderEnabled(@NonNull String provider) {

            }
        };

        startLocationListener();
    }

    private void askToEnableGPS() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage("Please enable GPS").setCancelable(false).setPositiveButton("Enable GPS",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Intent callGPSSettingIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(callGPSSettingIntent);
                    }
                });

        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }

    @SuppressLint("SimpleDateFormat")
    public String format(LatLng latLng) {
        String address = getAddress(latLng);

        return address + "\n" +
               "Added at " + new SimpleDateFormat("HH:mm yyyy-MM-dd").format(new Date())  + "\n" +
               "Coordinates: " + Math.round(latLng.latitude * 100.0) / 100.0 + " " + Math.round(latLng.longitude * 100.0) / 100.0;
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        String markerText = format(latLng);

        addMarker(latLng, markerText, 10);

        MainActivity.latLngList.add(latLng);
        MainActivity.listElements.add(markerText);
        MainActivity.arrayAdapter.notifyDataSetChanged();

        preferences.edit().putString("locations", preferences.getString("locations", "") + latLng.latitude + " " + latLng.longitude + " ").apply();
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        Toast.makeText(this, marker.getTitle(), Toast.LENGTH_SHORT).show();
    }
}
