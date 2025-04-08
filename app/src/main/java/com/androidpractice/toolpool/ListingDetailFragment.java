package com.androidpractice.toolpool;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ListingDetailFragment extends Fragment {
    private static final String ARG_LISTING = "listing";
    private Listing listing;
    private DatabaseReference savedListingsRef;
    private DatabaseReference usersRef;
    private DatabaseReference toolRequestsRef;
    private boolean isSaved = false;
    private String listerUsername;

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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        savedListingsRef = FirebaseDatabase.getInstance().getReference("saved_listings");
        usersRef = FirebaseDatabase.getInstance().getReference("users");
        toolRequestsRef = FirebaseDatabase.getInstance().getReference("tool_requests");
        if (getArguments() != null) {
            listing = (Listing) getArguments().getSerializable(ARG_LISTING);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_listing_detail, container, false);

        if (listing == null) {
            return view;
        }

        ImageButton saveButton = view.findViewById(R.id.save_button);
        ViewPager2 viewPager = view.findViewById(R.id.images_view_pager);
        TextView imageCounterView = view.findViewById(R.id.image_counter);
        TextView titleView = view.findViewById(R.id.detail_title);
        TextView descriptionView = view.findViewById(R.id.detail_description);
        TextView categoryView = view.findViewById(R.id.detail_category);
        TextView conditionView = view.findViewById(R.id.detail_condition);
        TextView addressView = view.findViewById(R.id.detail_address);
        TextView depositView = view.findViewById(R.id.detail_deposit);
        TextView lendDateView = view.findViewById(R.id.detail_lend_date);
        TextView returnDateView = view.findViewById(R.id.detail_return_date);
        ImageView uploaderProfilePic = view.findViewById(R.id.uploader_profile_picture);
        TextView uploaderName = view.findViewById(R.id.uploader_name);
        Button messageButton = view.findViewById(R.id.message_button);
        Button requestToolButton = view.findViewById(R.id.request_tool_button);

        titleView.setText(listing.getTitle());
        descriptionView.setText("Description: " + listing.getDescription());
        categoryView.setText("Category: " + listing.getCategory());
        conditionView.setText("Condition: " + listing.getCondition());
        addressView.setText("Address: " + listing.getAddress());
        depositView.setText("Deposit: $" + String.format("%.2f", listing.getDeposit()));

        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        lendDateView.setText("Lend Date: " + dateFormat.format(new Date(listing.getLendDate())));
        returnDateView.setText("Return Date: " + dateFormat.format(new Date(listing.getReturnDate())));

        List<String> photoUrls = listing.getPhotoUrls();
        ImagePagerAdapter adapter = new ImagePagerAdapter(photoUrls);
        viewPager.setAdapter(adapter);

        int totalImages = (photoUrls != null && !photoUrls.isEmpty()) ? photoUrls.size() : 1;
        imageCounterView.setText(String.format(Locale.getDefault(), "Image %d of %d", 1, totalImages));

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                imageCounterView.setText(String.format(Locale.getDefault(), "Image %d of %d", position + 1, totalImages));
            }
        });

        loadUploaderProfile(uploaderProfilePic, uploaderName, messageButton);
        checkIfSaved(saveButton);
        saveButton.setOnClickListener(v -> toggleSaveListing(saveButton));
        messageButton.setOnClickListener(v -> startChat());
        requestToolButton.setOnClickListener(v -> requestTool());

        // Make uploader profile clickable
        uploaderProfilePic.setOnClickListener(v -> {
            if (isAdded()) {
                PublicProfileFragment publicProfileFragment = PublicProfileFragment.newInstance(listing.getUserId());
                ((homeActivity) requireActivity()).replaceFragment(publicProfileFragment);
            }
        });

        return view;
    }

    private void loadUploaderProfile(ImageView profilePic, TextView nameView, Button messageButton) {
        usersRef.child(listing.getUserId()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && isAdded()) {
                    String username = snapshot.child("name").getValue(String.class);
                    String profilePicUrl = snapshot.child("profilePictureUrl").getValue(String.class);

                    if (username != null) {
                        listerUsername = username;
                        nameView.setText(username);
                        messageButton.setText("Message " + username);
                    }

                    if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
                        Picasso.get()
                                .load(profilePicUrl)
                                .placeholder(R.drawable.ic_default_profile)
                                .error(R.drawable.ic_default_profile)
                                .into(profilePic);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Error loading uploader: " + error.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void checkIfSaved(ImageButton saveButton) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        savedListingsRef.child(currentUserId).child(listing.getListingId())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        isSaved = snapshot.exists();
                        saveButton.setImageResource(isSaved ?
                                R.drawable.ic_bookmark : R.drawable.ic_bookmark_border);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (isAdded()) {
                            Toast.makeText(requireContext(), "Error checking save status: " + error.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void toggleSaveListing(ImageButton saveButton) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (isSaved) {
            savedListingsRef.child(currentUserId).child(listing.getListingId())
                    .removeValue()
                    .addOnSuccessListener(aVoid -> {
                        if (isAdded()) {
                            isSaved = false;
                            saveButton.setImageResource(R.drawable.ic_bookmark_border);
                            Toast.makeText(requireContext(), "Listing unsaved", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (isAdded()) {
                            Toast.makeText(requireContext(), "Failed to unsave: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            savedListingsRef.child(currentUserId).child(listing.getListingId())
                    .setValue(true)
                    .addOnSuccessListener(aVoid -> {
                        if (isAdded()) {
                            isSaved = true;
                            saveButton.setImageResource(R.drawable.ic_bookmark);
                            Toast.makeText(requireContext(), "Listing saved", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (isAdded()) {
                            Toast.makeText(requireContext(), "Failed to save: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void startChat() {
        if (!isAdded()) return;

        if (listerUsername == null) {
            Toast.makeText(requireContext(), "Unable to start chat: Username not loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (currentUserId.equals(listing.getUserId())) {
            Toast.makeText(requireContext(), "You cannot message yourself", Toast.LENGTH_SHORT).show();
            return;
        }

        ChatFragment chatFragment = ChatFragment.newInstance(listing.getUserId(), listerUsername);
        ((homeActivity) requireActivity()).replaceFragment(chatFragment);
    }

    private void requestTool() {
        if (!isAdded()) return;

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (currentUserId == null) {
            Toast.makeText(requireContext(), "Please log in to request a tool", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUserId.equals(listing.getUserId())) {
            Toast.makeText(requireContext(), "You cannot request your own tool", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check for existing pending request
        toolRequestsRef.orderByChild("requesterId").equalTo(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean hasPendingRequest = false;
                for (DataSnapshot requestSnapshot : snapshot.getChildren()) {
                    String existingListingId = requestSnapshot.child("listingId").getValue(String.class);
                    String status = requestSnapshot.child("status").getValue(String.class);
                    if (listing.getListingId().equals(existingListingId) && "pending".equals(status)) {
                        hasPendingRequest = true;
                        break;
                    }
                }

                if (hasPendingRequest) {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "You already have a pending request for this tool", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Create new request
                    String requestId = toolRequestsRef.push().getKey();
                    if (requestId == null) {
                        if (isAdded()) {
                            Toast.makeText(requireContext(), "Failed to generate request ID", Toast.LENGTH_SHORT).show();
                        }
                        return;
                    }

                    Map<String, Object> requestData = new HashMap<>();
                    requestData.put("requesterId", currentUserId);
                    requestData.put("listingId", listing.getListingId());
                    requestData.put("listerId", listing.getUserId());
                    requestData.put("timestamp", System.currentTimeMillis());
                    requestData.put("status", "pending");

                    toolRequestsRef.child(requestId).setValue(requestData)
                            .addOnSuccessListener(aVoid -> {
                                if (isAdded()) {
                                    Toast.makeText(requireContext(), "Tool request sent", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(e -> {
                                if (isAdded()) {
                                    Toast.makeText(requireContext(), "Failed to send request: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Error checking requests: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}