package com.androidpractice.toolpool;

import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MapDialogFragment extends DialogFragment implements OnMapReadyCallback {

    private MapView mapView;
    private GoogleMap mMap;

    // Default location (UBC Kelowna approximate)
    private static final LatLng DEFAULT_LOCATION = new LatLng(49.8864, -119.4969);
    // Default radius if none is passed (5000 m = 5 km)
    private static final double DEFAULT_RADIUS_METERS = 5000;
    // The radius to use (passed via arguments)
    private double selectedRadius = DEFAULT_RADIUS_METERS;
    // The currently selected location (if previously chosen; otherwise default)
    private LatLng selectedLocation = DEFAULT_LOCATION;

    private Marker selectedMarker;
    private Circle searchRadiusCircle;
    private Button btnConfirm;

    // Callback interface for location selection
    private OnLocationSelectedListener listener;
    public void setOnLocationSelectedListener(OnLocationSelectedListener listener) {
        this.listener = listener;
    }

    public MapDialogFragment() {
        // Required empty public constructor
    }

    /**
     * Create a new instance of MapDialogFragment with a specified radius and an optional pre-selected location.
     * @param radius The radius in meters.
     * @param location The previously selected location; if null, the default location is used.
     */
    public static MapDialogFragment newInstance(double radius, LatLng location) {
        MapDialogFragment fragment = new MapDialogFragment();
        Bundle args = new Bundle();
        args.putDouble("radius", radius);
        if (location != null) {
            args.putParcelable("location", location);
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Retrieve the radius and location from arguments if provided.
        if (getArguments() != null) {
            selectedRadius = getArguments().getDouble("radius", DEFAULT_RADIUS_METERS);
            LatLng loc = getArguments().getParcelable("location");
            if (loc != null) {
                selectedLocation = loc;
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map_dialog, container, false);
        mapView = view.findViewById(R.id.mapView);
        btnConfirm = view.findViewById(R.id.btnConfirm);

        // Initialize the Maps SDK.
        try {
            MapsInitializer.initialize(requireContext());
        } catch (Exception e) {
            e.printStackTrace();
        }
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        btnConfirm.setOnClickListener(v -> {
            if (selectedMarker != null) {
                LatLng chosenLatLng = selectedMarker.getPosition();
                String address = getAddressFromLatLng(chosenLatLng);
                if (listener != null) {
                    listener.onLocationSelected(chosenLatLng, address);
                }
                dismiss();
            } else {
                Toast.makeText(getContext(), "Please select a location on the map", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // Move the camera to the selected location (or default) with a zoom level that shows a larger area.
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLocation, 11));

        // Initially, add a marker and circle at the selected location.
        selectedMarker = mMap.addMarker(new MarkerOptions()
                .position(selectedLocation)
                .title("Selected Location"));
        searchRadiusCircle = mMap.addCircle(new CircleOptions()
                .center(selectedLocation)
                .radius(selectedRadius)
                .strokeWidth(4)
                .strokeColor(0xFFAAAAAA)  // Light gray outline.
                .fillColor(0x22AAAAAA));  // Semi-transparent light gray fill.

        // Listen for map taps to update the marker and circle.
        mMap.setOnMapClickListener(latLng -> {
            selectedLocation = latLng;
            if (selectedMarker != null) {
                selectedMarker.remove();
            }
            if (searchRadiusCircle != null) {
                searchRadiusCircle.remove();
            }
            selectedMarker = mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("Selected Location"));
            searchRadiusCircle = mMap.addCircle(new CircleOptions()
                    .center(latLng)
                    .radius(selectedRadius)
                    .strokeWidth(4)
                    .strokeColor(0xFFAAAAAA)
                    .fillColor(0x22AAAAAA));
        });
    }

    // Helper method to reverse geocode a LatLng into a human-readable address.
    private String getAddressFromLatLng(LatLng latLng) {
        Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address addr = addresses.get(0);
                return addr.getAddressLine(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Lat: " + latLng.latitude + ", Lng: " + latLng.longitude;
    }

    // Forward lifecycle events to the MapView.
    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }
    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }
    @Override
    public void onPause() {
        mapView.onPause();
        super.onPause();
    }
    @Override
    public void onStop() {
        mapView.onStop();
        super.onStop();
    }
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
    @Override
    public void onDestroy() {
        mapView.onDestroy();
        super.onDestroy();
    }
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    // Callback interface definition.
    public interface OnLocationSelectedListener {
        void onLocationSelected(LatLng latLng, String address);
    }
}
