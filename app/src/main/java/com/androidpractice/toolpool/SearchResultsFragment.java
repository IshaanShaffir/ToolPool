package com.androidpractice.toolpool;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.bumptech.glide.Glide;
import com.androidpractice.toolpool.databinding.FragmentSearchResultsBinding;
import java.util.List;

public class SearchResultsFragment extends Fragment {
    private FragmentSearchResultsBinding binding;
    private DatabaseReference databaseReference;
    private ValueEventListener listingsListener;
    private String searchQuery = "";

    public SearchResultsFragment() {
        // Required empty public constructor
    }

    // Factory method to create a new instance with the search query
    public static SearchResultsFragment newInstance(String searchQuery) {
        SearchResultsFragment fragment = new SearchResultsFragment();
        Bundle args = new Bundle();
        args.putString("searchQuery", searchQuery);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Retrieve the search query from arguments if available.
        if (getArguments() != null) {
            searchQuery = getArguments().getString("searchQuery", "");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentSearchResultsBinding.inflate(inflater, container, false);
        // Set up Firebase reference to "listings"
        databaseReference = FirebaseDatabase.getInstance().getReference("listings");
        fetchListings();
        return binding.getRoot();
    }

    private void fetchListings() {
        listingsListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!isAdded() || getContext() == null || binding == null) {
                    return;
                }
                // Clear any existing views.
                binding.listingsContainer.removeAllViews();

                // Iterate over all listings from Firebase.
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Listing listing = snapshot.getValue(Listing.class);
                    if (listing != null) {
                        // If searchQuery is empty, show all listings.
                        // Otherwise, check if the listing's title contains the search query (case-insensitive).
                        if (searchQuery.isEmpty() ||
                                listing.getTitle().toLowerCase().contains(searchQuery.toLowerCase())) {
                            addListingCard(listing);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                if (isAdded() && getContext() != null) {
                    Toast.makeText(getContext(), "Failed to load listings: " +
                            databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        };
        databaseReference.addValueEventListener(listingsListener);
    }

    private void addListingCard(Listing listing) {
        if (getContext() == null || binding == null) {
            return;
        }
        // Inflate the listing card layout.
        View cardView = LayoutInflater.from(getContext())
                .inflate(R.layout.listings_card, binding.listingsContainer, false);

        ImageView imageView = cardView.findViewById(R.id.listing_image);
        TextView titleView = cardView.findViewById(R.id.listing_title);

        titleView.setText(listing.getTitle());

        // Load the first photo if available.
        List<String> photoUrls = listing.getPhotoUrls();
        if (photoUrls != null && !photoUrls.isEmpty()) {
            Glide.with(getContext())
                    .load(photoUrls.get(0))
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .into(imageView);
        } else {
            imageView.setImageResource(R.drawable.placeholder_image);
        }

        // Set a click listener to navigate to ListingDetailFragment.
        cardView.setOnClickListener(v -> {
            if (getActivity() instanceof homeActivity) {
                ListingDetailFragment detailFragment = ListingDetailFragment.newInstance(listing);
                ((homeActivity) getActivity()).replaceFragment(detailFragment);
            }
        });

        binding.listingsContainer.addView(cardView);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (databaseReference != null && listingsListener != null) {
            databaseReference.removeEventListener(listingsListener);
        }
        binding = null;
    }
}
