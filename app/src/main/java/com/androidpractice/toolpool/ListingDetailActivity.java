package com.androidpractice.toolpool;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ListingDetailActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listing_detail);

        // Get the Listing object from the Intent
        Listing listing = (Listing) getIntent().getSerializableExtra("listing");

        if (listing == null) {
            finish(); // Close activity if no listing data
            return;
        }

        // Bind views
        ImageView imageView = findViewById(R.id.detail_image);
        TextView titleView = findViewById(R.id.detail_title);
        TextView descriptionView = findViewById(R.id.detail_description);
        TextView categoryView = findViewById(R.id.detail_category);
        TextView addressView = findViewById(R.id.detail_address);
        TextView depositView = findViewById(R.id.detail_deposit);
        TextView lendDateView = findViewById(R.id.detail_lend_date);
        TextView returnDateView = findViewById(R.id.detail_return_date);

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

        // Load first photo if available
        List<String> photoUrls = listing.getPhotoUrls();
        if (photoUrls != null && !photoUrls.isEmpty()) {
            Glide.with(this)
                    .load(photoUrls.get(0))
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .into(imageView);
        } else {
            imageView.setImageResource(R.drawable.placeholder_image);
        }
    }
}