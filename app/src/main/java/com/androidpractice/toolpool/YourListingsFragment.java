package com.androidpractice.toolpool;

import android.app.AlertDialog;
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

public class YourListingsFragment extends Fragment {
    private RecyclerView recyclerView;
    private MyListingsAdapter adapter;
    private DatabaseReference listingsRef;
    private DatabaseReference toolRequestsRef;

    public YourListingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        listingsRef = FirebaseDatabase.getInstance().getReference("listings");
        toolRequestsRef = FirebaseDatabase.getInstance().getReference("tool_requests");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_your_listings, container, false);

        recyclerView = view.findViewById(R.id.my_listings_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new MyListingsAdapter();
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(listing -> {
            if (isAdded()) {
                showListingOptionsDialog(listing);
            }
        });

        loadListings();
        return view;
    }

    private void loadListings() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        listingsRef.orderByChild("userId").equalTo(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Listing> userListings = new ArrayList<>();
                for (DataSnapshot listingSnapshot : snapshot.getChildren()) {
                    Listing listing = listingSnapshot.getValue(Listing.class);
                    if (listing != null) {
                        listing.setListingId(listingSnapshot.getKey());
                        userListings.add(listing);
                    }
                }
                adapter.setListings(userListings);
                if (userListings.isEmpty() && isAdded()) {
                    Toast.makeText(requireContext(), "You have no listings yet", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Error loading listings: " + error.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showListingOptionsDialog(Listing listing) {
        String[] options;
        if (listing.isBooked()) {
            options = new String[]{"Edit Listing", "Mark as Available", "Mark as Returned"};
        } else {
            options = new String[]{"Edit Listing", "Mark as Booked"};
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(listing.getTitle())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // Edit Listing
                        EditListingFragment editFragment = EditListingFragment.newInstance(listing);
                        ((homeActivity) requireActivity()).replaceFragment(editFragment);
                    } else if (which == 1) {
                        // Mark as Booked or Available
                        toggleBookedStatus(listing);
                    } else if (which == 2 && listing.isBooked()) {
                        // Mark as Returned (only for booked listings)
                        markAsReturned(listing);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void toggleBookedStatus(Listing listing) {
        boolean newStatus = !listing.isBooked();
        listingsRef.child(listing.getListingId()).child("booked").setValue(newStatus)
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(),
                                newStatus ? "Listing marked as booked" : "Listing marked as available",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Failed to update status: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void markAsReturned(Listing listing) {
        // Find the requesterId from the accepted tool request
        toolRequestsRef.orderByChild("listingId").equalTo(listing.getListingId())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String requesterId = null;
                        for (DataSnapshot requestSnapshot : snapshot.getChildren()) {
                            String status = requestSnapshot.child("status").getValue(String.class);
                            if ("accepted".equals(status)) {
                                requesterId = requestSnapshot.child("requesterId").getValue(String.class);
                                break;
                            }
                        }

                        if (requesterId != null && isAdded()) {
                            ReviewFragment reviewFragment = ReviewFragment.newInstance(listing.getListingId(), requesterId);
                            ((homeActivity) requireActivity()).replaceFragment(reviewFragment);
                        } else if (isAdded()) {
                            Toast.makeText(requireContext(), "No accepted request found for this listing", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (isAdded()) {
                            Toast.makeText(requireContext(), "Error finding request: " + error.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}