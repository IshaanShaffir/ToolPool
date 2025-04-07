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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends Fragment {
    private RecyclerView recyclerView;
    private MyListingsAdapter adapter;
    private DatabaseReference listingsRef;
    private FirebaseUser currentUser;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        listingsRef = FirebaseDatabase.getInstance().getReference("listings");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        recyclerView = view.findViewById(R.id.my_listings_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new MyListingsAdapter();
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(listing -> {
            EditListingFragment editFragment = EditListingFragment.newInstance(listing);
            ((homeActivity) requireActivity()).replaceFragment(editFragment);
        });

        if (currentUser != null) {
            loadUserListings();
        } else {
            Toast.makeText(requireContext(), "Please log in to view your listings", Toast.LENGTH_SHORT).show();
        }

        return view;
    }

    private void loadUserListings() {
        String userId = currentUser.getUid();
        listingsRef.orderByChild("userId").equalTo(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Listing> userListings = new ArrayList<>();
                for (DataSnapshot listingSnapshot : snapshot.getChildren()) {
                    Listing listing = listingSnapshot.getValue(Listing.class);
                    if (listing != null) {
                        userListings.add(listing);
                    }
                }
                adapter.setListings(userListings);
                if (userListings.isEmpty()) {
                    Toast.makeText(requireContext(), "You have no listings yet", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), "Error loading listings: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}