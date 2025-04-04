package com.androidpractice.toolpool;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ListingDetailFragment extends Fragment {
    private static final String ARG_LISTING = "listing";

    public ListingDetailFragment() {
        // Required empty public constructor
    }

    public static ListingDetailFragment newInstance(Listing listing) {
        ListingDetailFragment fragment = new ListingDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_LISTING, listing);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_listing_detail, container, false);

        // Get the Listing object from arguments
        Listing listing = (Listing) getArguments().getSerializable(ARG_LISTING);
        if (listing == null) {
            return view; // Return empty view if no data
        }

        // Bind views
        ViewPager2 viewPager = view.findViewById(R.id.images_view_pager);
        TextView imageCounterView = view.findViewById(R.id.image_counter);
        TextView titleView = view.findViewById(R.id.detail_title);
        TextView descriptionView = view.findViewById(R.id.detail_description);
        TextView categoryView = view.findViewById(R.id.detail_category);
        TextView addressView = view.findViewById(R.id.detail_address);
        TextView depositView = view.findViewById(R.id.detail_deposit);
        TextView lendDateView = view.findViewById(R.id.detail_lend_date);
        TextView returnDateView = view.findViewById(R.id.detail_return_date);

        // Set data
        titleView.setText(listing.getTitle());
        descriptionView.setText("Description: " + listing.getDescription());
        categoryView.setText("Category: " + listing.getCategory());
        addressView.setText("Address: " + listing.getAddress());
        depositView.setText("Deposit: $" + String.format("%.2f", listing.getDeposit()));

        // Format dates
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        lendDateView.setText("Lend Date: " + dateFormat.format(new Date(listing.getLendDate())));
        returnDateView.setText("Return Date: " + dateFormat.format(new Date(listing.getReturnDate())));

        // Set up ViewPager2 with images
        List<String> photoUrls = listing.getPhotoUrls();
        ImagePagerAdapter adapter = new ImagePagerAdapter(photoUrls);
        viewPager.setAdapter(adapter);

        // Update image counter
        int totalImages = (photoUrls != null && !photoUrls.isEmpty()) ? photoUrls.size() : 1;
        imageCounterView.setText(String.format(Locale.getDefault(), "Image %d of %d", 1, totalImages));

        // Update counter on page change
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                imageCounterView.setText(String.format(Locale.getDefault(), "Image %d of %d", position + 1, totalImages));
            }
        });

        return view;
    }
}