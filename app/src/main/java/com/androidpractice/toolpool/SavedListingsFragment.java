package com.androidpractice.toolpool;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class SavedListingsFragment extends Fragment {
    private RecyclerView recyclerView;
    private MyListingsAdapter adapter;
    private DatabaseReference savedListingsRef;
    private DatabaseReference listingsRef;

    public SavedListingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        savedListingsRef = FirebaseDatabase.getInstance().getReference("saved_listings");
        listingsRef = FirebaseDatabase.getInstance().getReference("listings");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_saved_listings, container, false);

        recyclerView = view.findViewById(R.id.saved_listings_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new MyListingsAdapter();
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(listing -> {
            if (isAdded()) {
                ListingDetailFragment detailFragment = ListingDetailFragment.newInstance(listing);
                ((homeActivity) requireActivity()).replaceFragment(detailFragment);
            }
        });

        loadSavedListings();
        return view;
    }

    private void loadSavedListings() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        savedListingsRef.child(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Listing> savedListings = new ArrayList<>();
                List<String> savedListingIds = new ArrayList<>();

                for (DataSnapshot savedSnapshot : snapshot.getChildren()) {
                    savedListingIds.add(savedSnapshot.getKey());
                }

                if (savedListingIds.isEmpty() && isAdded()) {
                    adapter.setListings(savedListings);
                    Toast.makeText(requireContext(), "You have no saved listings", Toast.LENGTH_SHORT).show();
                    return;
                }

                listingsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot listingsSnapshot) {
                        if (!isAdded()) return;

                        for (String listingId : savedListingIds) {
                            DataSnapshot listingSnapshot = listingsSnapshot.child(listingId);
                            if (listingSnapshot.exists()) {
                                Listing listing = listingSnapshot.getValue(Listing.class);
                                if (listing != null) {
                                    listing.setListingId(listingId);
                                    savedListings.add(listing);
                                }
                            }
                        }
                        adapter.setListings(savedListings);
                        if (savedListings.isEmpty() && isAdded()) {
                            Toast.makeText(requireContext(), "No valid saved listings found", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (isAdded()) {
                            Toast.makeText(requireContext(), "Error loading saved listings: " + error.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Error loading saved listings: " + error.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}