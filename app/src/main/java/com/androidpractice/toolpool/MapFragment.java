package com.androidpractice.toolpool;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MapFragment extends Fragment implements OnMapReadyCallback, GoogleMap.InfoWindowAdapter {
    private GoogleMap mMap;
    private DatabaseReference listingsRef;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final float DEFAULT_ZOOM = 12f;

    public MapFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        listingsRef = FirebaseDatabase.getInstance().getReference("listings");

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        return view;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.setInfoWindowAdapter(this);

        mMap.setOnInfoWindowClickListener(marker -> {
            Listing listing = (Listing) marker.getTag();
            if (listing != null) {
                ListingDetailFragment detailFragment = ListingDetailFragment.newInstance(listing);
                ((homeActivity) requireActivity()).replaceFragment(detailFragment);
                marker.hideInfoWindow();
            }
        });

        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        setupMap();
        getUserLocation();
    }

    private void setupMap() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                mMap.setMyLocationEnabled(true);
            } catch (SecurityException e) {
                Toast.makeText(requireContext(), "Location permission error: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            Toast.makeText(requireContext(), "Location permission not granted",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        listingsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                mMap.clear();

                for (DataSnapshot listingSnapshot : snapshot.getChildren()) {
                    Listing listing = listingSnapshot.getValue(Listing.class);
                    if (listing != null) {
                        LatLng position = new LatLng(listing.getLatitude(), listing.getLongitude());
                        Marker marker = mMap.addMarker(new MarkerOptions()
                                .position(position)
                                .title(listing.getTitle()));
                        if (marker != null) {
                            marker.setTag(listing);
                            // Preload the first image for faster display
                            if (listing.getPhotoUrls() != null && !listing.getPhotoUrls().isEmpty()) {
                                Glide.with(requireContext())
                                        .load(listing.getPhotoUrls().get(0))
                                        .apply(new RequestOptions()
                                                .override(100, 100) // Resize to thumbnail size
                                                .diskCacheStrategy(DiskCacheStrategy.ALL)) // Cache aggressively
                                        .preload();
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), "Error loading listings: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getUserLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(requireActivity(), new OnSuccessListener<android.location.Location>() {
                        @Override
                        public void onSuccess(android.location.Location location) {
                            if (location != null) {
                                LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, DEFAULT_ZOOM));
                            } else {
                                Toast.makeText(requireContext(), "Unable to get current location",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(requireContext(), "Location fetch failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        }
    }

    @Override
    public View getInfoWindow(Marker marker) {
        return null;
    }

    @Override
    public View getInfoContents(Marker marker) {
        Listing listing = (Listing) marker.getTag();
        if (listing == null) return null;

        View view = LayoutInflater.from(requireContext()).inflate(R.layout.custom_info_window, null);

        ImageView photoView = view.findViewById(R.id.info_photo);
        TextView descriptionView = view.findViewById(R.id.info_description);

        if (listing.getPhotoUrls() != null && !listing.getPhotoUrls().isEmpty()) {
            Glide.with(this)
                    .load(listing.getPhotoUrls().get(0))
                    .apply(new RequestOptions()
                            .override(100, 100) // Load thumbnail size
                            .diskCacheStrategy(DiskCacheStrategy.ALL) // Use cached version if available
                            .placeholder(android.R.drawable.ic_menu_gallery) // Show placeholder while loading
                            .error(android.R.drawable.ic_menu_gallery)) // Show fallback on error
                    .into(photoView);
        } else {
            photoView.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        descriptionView.setText(listing.getDescription());

        return view;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupMap();
                getUserLocation();
            } else {
                Toast.makeText(requireContext(), "Location permission denied",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
}