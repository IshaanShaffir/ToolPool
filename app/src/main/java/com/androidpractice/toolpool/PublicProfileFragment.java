package com.androidpractice.toolpool;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class PublicProfileFragment extends Fragment {
    private static final String ARG_USER_ID = "userId";
    private String userId;
    private DatabaseReference usersRef;
    private ImageView profilePicture;
    private TextView usernameText;
    private RatingBar averageRatingBar;
    private TextView ratingText;
    private RecyclerView reviewsRecyclerView;
    private ReviewsAdapter reviewsAdapter;

    public PublicProfileFragment() {
        // Required empty public constructor
    }

    public static PublicProfileFragment newInstance(String userId) {
        PublicProfileFragment fragment = new PublicProfileFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USER_ID, userId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        usersRef = FirebaseDatabase.getInstance().getReference("users");
        if (getArguments() != null) {
            userId = getArguments().getString(ARG_USER_ID);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_public_profile, container, false);

        profilePicture = view.findViewById(R.id.profile_picture);
        usernameText = view.findViewById(R.id.username_text);
        averageRatingBar = view.findViewById(R.id.average_rating_bar);
        ratingText = view.findViewById(R.id.rating_text);
        reviewsRecyclerView = view.findViewById(R.id.reviews_recycler_view);

        reviewsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        reviewsAdapter = new ReviewsAdapter();
        reviewsRecyclerView.setAdapter(reviewsAdapter);

        loadPublicProfile();
        return view;
    }

    private void loadPublicProfile() {
        usersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && isAdded()) {
                    String username = snapshot.child("name").getValue(String.class);
                    if (username != null) {
                        usernameText.setText(username);
                    }

                    String profilePicUrl = snapshot.child("profilePictureUrl").getValue(String.class);
                    if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
                        Picasso.get()
                                .load(profilePicUrl)
                                .placeholder(R.drawable.ic_default_profile)
                                .error(R.drawable.ic_default_profile)
                                .into(profilePicture);
                    } else {
                        profilePicture.setImageResource(R.drawable.ic_default_profile);
                    }

                    // Calculate average rating
                    DataSnapshot ratingsSnapshot = snapshot.child("ratings");
                    float totalRating = 0;
                    long ratingCount = ratingsSnapshot.getChildrenCount();

                    if (ratingCount > 0) {
                        for (DataSnapshot rating : ratingsSnapshot.getChildren()) {
                            Float value = rating.getValue(Float.class);
                            if (value != null) {
                                totalRating += value;
                            }
                        }
                        float averageRating = totalRating / ratingCount;
                        averageRatingBar.setRating(averageRating);
                        ratingText.setText(String.format("(%.1f)", averageRating));
                    } else {
                        averageRatingBar.setRating(0);
                        ratingText.setText("(0.0)");
                    }

                    // Load reviews (comments + ratings + reviewer info)
                    DataSnapshot commentsSnapshot = snapshot.child("comments");
                    List<Review> reviews = new ArrayList<>();
                    for (DataSnapshot comment : commentsSnapshot.getChildren()) {
                        String commentText = comment.getValue(String.class);
                        if (commentText != null) {
                            // Placeholder reviewerId - adjust if linked
                            String reviewerId = "unknown";
                            Float rating = getRatingForComment(ratingsSnapshot, comment.getKey());
                            reviews.add(new Review(reviewerId, commentText, rating != null ? rating : 0f));
                        }
                    }
                    fetchReviewerDetails(reviews);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Error loading profile: " + error.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private Float getRatingForComment(DataSnapshot ratingsSnapshot, String commentId) {
        // Simplified; improve with reviewerId linkage if available
        for (DataSnapshot rating : ratingsSnapshot.getChildren()) {
            return rating.getValue(Float.class); // Returns first rating for now
        }
        return null;
    }

    private void fetchReviewerDetails(List<Review> reviews) {
        List<Review> updatedReviews = new ArrayList<>(reviews);
        for (Review review : updatedReviews) {
            usersRef.child(review.reviewerId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists() && isAdded()) {
                        String profilePicUrl = snapshot.child("profilePictureUrl").getValue(String.class);
                        review.profilePicUrl = profilePicUrl;
                        reviewsAdapter.setReviews(updatedReviews);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Error loading reviewer: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
        reviewsAdapter.setReviews(updatedReviews);
    }

    private static class Review {
        String reviewerId;
        String comment;
        float rating;
        String profilePicUrl;

        Review(String reviewerId, String comment, float rating) {
            this.reviewerId = reviewerId;
            this.comment = comment;
            this.rating = rating;
        }
    }

    private static class ReviewsAdapter extends RecyclerView.Adapter<ReviewsAdapter.ViewHolder> {
        private List<Review> reviews = new ArrayList<>();

        void setReviews(List<Review> reviews) {
            this.reviews = reviews;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_review, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Review review = reviews.get(position);
            holder.commentText.setText(review.comment);
            holder.ratingBar.setRating(review.rating);
            if (review.profilePicUrl != null && !review.profilePicUrl.isEmpty()) {
                Picasso.get()
                        .load(review.profilePicUrl)
                        .placeholder(R.drawable.ic_default_profile)
                        .error(R.drawable.ic_default_profile)
                        .into(holder.profilePic);
            } else {
                holder.profilePic.setImageResource(R.drawable.ic_default_profile);
            }
        }

        @Override
        public int getItemCount() {
            return reviews.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView profilePic;
            TextView commentText;
            RatingBar ratingBar;

            ViewHolder(View itemView) {
                super(itemView);
                profilePic = itemView.findViewById(R.id.reviewer_profile_pic);
                commentText = itemView.findViewById(R.id.review_comment);
                ratingBar = itemView.findViewById(R.id.review_rating);
            }
        }
    }
}