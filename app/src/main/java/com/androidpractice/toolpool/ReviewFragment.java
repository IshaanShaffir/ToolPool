package com.androidpractice.toolpool;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class ReviewFragment extends Fragment {
    private static final String ARG_LISTING_ID = "listingId";
    private static final String ARG_REQUESTER_ID = "requesterId";
    private String listingId;
    private String requesterId;
    private DatabaseReference usersRef;
    private DatabaseReference listingsRef;

    public ReviewFragment() {
        // Required empty public constructor
    }

    public static ReviewFragment newInstance(String listingId, String requesterId) {
        ReviewFragment fragment = new ReviewFragment();
        Bundle args = new Bundle();
        args.putString(ARG_LISTING_ID, listingId);
        args.putString(ARG_REQUESTER_ID, requesterId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        usersRef = FirebaseDatabase.getInstance().getReference("users");
        listingsRef = FirebaseDatabase.getInstance().getReference("listings");
        if (getArguments() != null) {
            listingId = getArguments().getString(ARG_LISTING_ID);
            requesterId = getArguments().getString(ARG_REQUESTER_ID);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_review, container, false);

        RatingBar ratingBar = view.findViewById(R.id.rating_bar);
        EditText commentEditText = view.findViewById(R.id.comment_edit_text);
        Button submitButton = view.findViewById(R.id.submit_review_button);

        submitButton.setOnClickListener(v -> submitReview(ratingBar.getRating(), commentEditText.getText().toString()));

        return view;
    }

    private void submitReview(float rating, String comment) {
        if (!isAdded()) return;

        DatabaseReference requesterRef = usersRef.child(requesterId);
        Map<String, Object> updates = new HashMap<>();

        // Add individual rating
        String ratingId = requesterRef.child("ratings").push().getKey();
        updates.put("ratings/" + ratingId, rating);

        // Add comment if provided
        if (!comment.isEmpty()) {
            String commentId = requesterRef.child("comments").push().getKey();
            updates.put("comments/" + commentId, comment);
        }

        requesterRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    // Mark listing as available
                    listingsRef.child(listingId).child("booked").setValue(false)
                            .addOnSuccessListener(aVoid2 -> {
                                if (isAdded()) {
                                    Toast.makeText(requireContext(), "Review submitted and listing marked as available", Toast.LENGTH_SHORT).show();
                                    ((homeActivity) requireActivity()).replaceFragment(new YourListingsFragment());
                                }
                            })
                            .addOnFailureListener(e -> {
                                if (isAdded()) {
                                    Toast.makeText(requireContext(), "Failed to mark listing as available: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Failed to submit review: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}