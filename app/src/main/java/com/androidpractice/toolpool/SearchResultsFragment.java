package com.androidpractice.toolpool;

import android.os.Bundle;
import android.util.Log;
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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SearchResultsFragment extends Fragment {
    private FragmentSearchResultsBinding binding;
    private DatabaseReference databaseReference;
    private ValueEventListener listingsListener;
    private String searchQuery = "";
    private String searchCategory = "";
    private String searchCondition = "";
    private boolean filterByLocation = false;
    private double filterLat = 0;
    private double filterLng = 0;
    private double filterRadius = 0; // in meters
    private long lendDateTimestamp = -1;
    private long returnDateTimestamp = -1;
    private double filterDeposit = -1;

    public SearchResultsFragment() {
        // Required empty public constructor
    }
    // create new instance of fragment with search parameters
    public static SearchResultsFragment newInstance(String searchQuery, String category, String condition,
                                                    boolean filterByLocation, double lat, double lng, double radius,
                                                    long lendDate, long returnDate, double deposit) {
        SearchResultsFragment fragment = new SearchResultsFragment();
        Bundle args = new Bundle();
        args.putString("searchQuery", searchQuery);
        args.putString("searchCategory", category);
        args.putString("searchCondition", condition);
        args.putBoolean("filterByLocation", filterByLocation);
        args.putDouble("filterLat", lat);
        args.putDouble("filterLng", lng);
        args.putDouble("filterRadius", radius);
        args.putLong("filterLendDate", lendDate);
        args.putLong("filterReturnDate", returnDate);
        args.putDouble("filterDeposit", deposit);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            searchQuery = getArguments().getString("searchQuery", "");
            searchCategory = getArguments().getString("searchCategory", "");
            searchCondition = getArguments().getString("searchCondition", "");
            filterByLocation = getArguments().getBoolean("filterByLocation", false);
            filterLat = getArguments().getDouble("filterLat", 0);
            filterLng = getArguments().getDouble("filterLng", 0);
            filterRadius = getArguments().getDouble("filterRadius", 0);
            lendDateTimestamp = getArguments().getLong("filterLendDate", -1);
            returnDateTimestamp = getArguments().getLong("filterReturnDate", -1);
            filterDeposit = getArguments().getDouble("filterDeposit", -1);
        }
    }

    // get connection to database and fetch listings
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentSearchResultsBinding.inflate(inflater, container, false);
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
                // use device timezone to compare dates (also used on home screen)
                SimpleDateFormat compareFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
                String searchLendStr = "";
                String searchReturnStr = "";
                if (lendDateTimestamp > 0) {
                    searchLendStr = compareFormat.format(new Date(lendDateTimestamp));
                }
                if (returnDateTimestamp > 0) {
                    searchReturnStr = compareFormat.format(new Date(returnDateTimestamp));
                }

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Log.d("SearchResults", "Database returned listing: " + snapshot.getValue());
                    Listing listing = snapshot.getValue(Listing.class);
                    if (listing != null) {
                        boolean matchesQuery = searchQuery.isEmpty() || // defaults to true if no query is set or the title matches
                                listing.getTitle().toLowerCase().contains(searchQuery.toLowerCase());
                        boolean matchesCategory = searchCategory.isEmpty() ||
                                listing.getCategory().equalsIgnoreCase(searchCategory);
                        boolean matchesCondition = searchCondition.isEmpty() ||
                                listing.getCondition().equalsIgnoreCase(searchCondition);

                        boolean matchesLocation = true; // defaults to true if no location is set
                        if (filterByLocation) {
                            double distance = distanceBetween(listing.getLatitude(), listing.getLongitude(),
                                    filterLat, filterLng);
                            matchesLocation = distance <= filterRadius;
                        }

                        boolean matchesDates = true; // defaults to true if no dates are set
                        if (!searchLendStr.isEmpty() && !searchReturnStr.isEmpty()) {
                            String listingLendStr = compareFormat.format(new Date(listing.getLendDate()));
                            String listingReturnStr = compareFormat.format(new Date(listing.getReturnDate()));
                            matchesDates = listingLendStr.compareTo(searchLendStr) <= 0 &&
                                    listingReturnStr.compareTo(searchReturnStr) >= 0;
                        }

                        boolean matchesDeposit = true;
                        if (filterDeposit >= 0) { // defaults to -1 if not set
                            matchesDeposit = listing.getDeposit() <= filterDeposit;
                        }
                        // check all conditions
                        if (matchesQuery && matchesCategory && matchesCondition && matchesLocation && matchesDates && matchesDeposit) {
                            addListingCard(listing);
                        }
                    }
                }
                if (binding.listingsContainer.getChildCount() == 0) { // fall back to no results text if no hits
                    binding.noResultsTextView.setVisibility(View.VISIBLE);
                } else {
                    binding.noResultsTextView.setVisibility(View.GONE);
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
    // use Haversine formula to calculate distance between two points on the earth
    private double distanceBetween(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // earth radius in meters
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
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
