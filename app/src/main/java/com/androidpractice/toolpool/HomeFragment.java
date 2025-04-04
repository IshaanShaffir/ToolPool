package com.androidpractice.toolpool;

import android.content.Intent;
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
import com.androidpractice.toolpool.databinding.FragmentHomeBinding;

import java.util.List;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;
    private DatabaseReference databaseReference;
    private ValueEventListener listingsListener;

    public HomeFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
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
                binding.listingsContainer.removeAllViews();

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
                    Toast.makeText(getContext(), "Failed to load listings: " + databaseError.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        };
        databaseReference.addValueEventListener(listingsListener);
    }

    private void addListingCard(Listing listing) {
        if (getContext() == null || binding == null) {
            return;
        }

        View cardView = LayoutInflater.from(getContext())
                .inflate(R.layout.listings_card, binding.listingsContainer, false);

        ImageView imageView = cardView.findViewById(R.id.listing_image);
        TextView titleView = cardView.findViewById(R.id.listing_title);

        titleView.setText(listing.getTitle());

        // Load first photo if available
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

        // Add click listener to the card
        cardView.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), ListingDetailActivity.class);
            intent.putExtra("listing", listing); // Assumes Listing is Serializable or Parcelable
            startActivity(intent);
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