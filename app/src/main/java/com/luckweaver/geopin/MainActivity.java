package com.luckweaver.geopin;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.*;
import android.location.Geocoder;
import android.os.*;
import android.view.*;
import android.widget.*;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    ListView savedLocationsList;

    SharedPreferences preferences;

    static ArrayList<LatLng> latLngList = new ArrayList<>();
    static ArrayList<String> listElements = new ArrayList<>();
    static ArrayAdapter<String> arrayAdapter;

    public void goToMap(View view) {
        Intent intent = new Intent(getApplicationContext(), MapsActivity.class);
        startActivity(intent);
    }

    public void goToMap(int i) {
        Intent intent = new Intent(getApplicationContext(), MapsActivity.class);
        intent.putExtra("show_index", i);
        startActivity(intent);
    }

    // public void clearPreferences() {
    //     SharedPreferences preferences = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
    //     preferences.edit().clear().apply();
    // }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        savedLocationsList = findViewById(R.id.savedLocationsList);

        arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listElements);
        savedLocationsList.setAdapter(arrayAdapter);

        savedLocationsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                goToMap(i);
            }
        });

        savedLocationsList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                deleteValue(i);
                return true;
            }
        });

        preferences = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
        MapsActivity mapsActivity = new MapsActivity();
        mapsActivity.geocoder = new Geocoder(this, Locale.getDefault());
        mapsActivity.initialize(preferences);
    }

    private void deleteValue(final int i) {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage("Do you want to delete this place?").setCancelable(true);

        alertDialogBuilder.setPositiveButton("Delete",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        latLngList.remove(i);
                        listElements.remove(i);
                        arrayAdapter.notifyDataSetChanged();

                        String[] savedLocations = Objects.requireNonNull(preferences.getString("locations", "")).trim().split(" ");

                        StringBuilder newString = new StringBuilder();

                        for(int j = 0; j < savedLocations.length; j++)
                            if (!(j == i*2 || j == i*2+1))
                                newString.append(savedLocations[j]).append(" ");

                        preferences.edit().putString("locations", newString.toString()).apply();
                    }
                });

        alertDialogBuilder.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }
}
