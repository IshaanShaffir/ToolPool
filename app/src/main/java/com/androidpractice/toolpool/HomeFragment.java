package com.androidpractice.toolpool;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.androidpractice.toolpool.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;
    private DatabaseReference databaseReference;
    private ValueEventListener listingsListener; // Store the listener to remove it later

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        databaseReference = FirebaseDatabase.getInstance().getReference("listings");

        // Fetch and display listings
        fetchListings();

        return binding.getRoot();
    }

    private void fetchListings() {
        listingsListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Ensure the fragment is attached and has a valid context
                if (!isAdded() || getContext() == null || binding == null) {
                    return; // Skip if fragment is not attached or view is destroyed
                }

                // Clear existing views
                binding.listingsContainer.removeAllViews();

                /* Remove any existing views */
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Listing listing = snapshot.getValue(Listing.class);
                    if (listing != null) {
                        addListingCard(listing);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                if (isAdded() && getContext() != null) {
                    android.widget.Toast.makeText(getContext(), "Failed to load listings: " + databaseError.getMessage(),
                            android.widget.Toast.LENGTH_SHORT).show();
                }
            }
        };
        databaseReference.addValueEventListener(listingsListener);
    }

    private void addListingCard(Listing listing) {
        // Use the fragment's view context or activity context
        if (getContext() == null || binding == null) {
            return; // Skip if no valid context
        }

        // Inflate the card view layout
        View cardView = LayoutInflater.from(getContext())
                .inflate(R.layout.listings_card, binding.listingsContainer, false);

        // Find views in the card
        TextView titleView = cardView.findViewById(R.id.listing_title);

        // Set the listing title
        titleView.setText(listing.getTitle());

        // Add the card to the container
        binding.listingsContainer.addView(cardView);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Remove the listener when the view is destroyed
        if (databaseReference != null && listingsListener != null) {
            databaseReference.removeEventListener(listingsListener);
        }
        binding = null; // Clear binding reference
    }
}