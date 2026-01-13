package com.example.stepbuystep.ActivityCoach.CoachCreateScreen;

import android.app.Activity;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.stepbuystep.R;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MapPickerActivity extends AppCompatActivity {

    private MapView map;
    private GeoPoint selectedLocation;
    private Marker marker;
    private Button btnConfirmLocation, btnSearch;
    private EditText etSearchAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Configure OSMDroid
        Configuration.getInstance().load(this, PreferenceManager. getDefaultSharedPreferences(this));

        setContentView(R.layout.activity_map_picker);

        btnConfirmLocation = findViewById(R.id. btnConfirmLocation);
        btnSearch = findViewById(R.id. btnSearch);
        etSearchAddress = findViewById(R.id.etSearchAddress);
        map = findViewById(R.id.map);

        // Setup map
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getController().setZoom(12.0);

        // Default location (Tel Aviv, Israel - you can change this)
        selectedLocation = new GeoPoint(32.0853, 34.7818);
        map.getController().setCenter(selectedLocation);

        // Add marker
        marker = new Marker(map);
        marker.setPosition(selectedLocation);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle("Selected Location");
        map.getOverlays().add(marker);

        // Handle map clicks
        MapEventsReceiver mapEventsReceiver = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                selectedLocation = p;
                marker.setPosition(p);
                map.invalidate();

                // Update the address in search field
                String address = getAddressFromLatLng(p.getLatitude(), p.getLongitude());
                etSearchAddress.setText(address);

                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        };

        MapEventsOverlay eventsOverlay = new MapEventsOverlay(mapEventsReceiver);
        map.getOverlays().add(0, eventsOverlay);

        btnSearch.setOnClickListener(v -> searchAddress());
        btnConfirmLocation.setOnClickListener(v -> confirmLocation());
    }

    private void searchAddress() {
        String addressText = etSearchAddress.getText().toString().trim();

        if (addressText.isEmpty()) {
            Toast.makeText(this, "Please enter an address", Toast. LENGTH_SHORT).show();
            return;
        }

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(addressText, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                double latitude = address.getLatitude();
                double longitude = address.getLongitude();

                selectedLocation = new GeoPoint(latitude, longitude);
                marker.setPosition(selectedLocation);
                map.getController().setCenter(selectedLocation);
                map.getController().setZoom(15.0);
                map.invalidate();

                Toast.makeText(this, "Location found!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Address not found.  Try a different search.", Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error searching address:  " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void confirmLocation() {
        if (selectedLocation == null) {
            Toast.makeText(this, "Please select a location", Toast.LENGTH_SHORT).show();
            return;
        }

        String address = getAddressFromLatLng(selectedLocation.getLatitude(), selectedLocation.getLongitude());

        Intent resultIntent = new Intent();
        resultIntent.putExtra("latitude", selectedLocation.getLatitude());
        resultIntent.putExtra("longitude", selectedLocation.getLongitude());
        resultIntent.putExtra("address", address);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }

    private String getAddressFromLatLng(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                return address.getAddressLine(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return String.format("%.6f, %.6f", latitude, longitude);
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
    }
}