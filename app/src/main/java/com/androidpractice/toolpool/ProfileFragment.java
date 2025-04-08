package com.androidpractice.toolpool;

import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

public class ProfileFragment extends Fragment {
    private ImageView profilePicture;
    private TextView usernameText;
    private RatingBar userRatingBar;
    private TextView ratingText;
    private Button yourListingsButton;
    private Button savedListingsButton;
    private Button chatHistoryButton;
    private Button toolRequestsButton;
    private Button publicProfileButton;
    private DatabaseReference usersRef;
    private FirebaseUser currentUser;
    private StorageReference storageReference;

    private final ActivityResultLauncher<String> photoPicker = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    uploadProfilePicture(uri);
                }
            });

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        usersRef = FirebaseDatabase.getInstance().getReference("users");
        storageReference = FirebaseStorage.getInstance().getReference("profile_pictures");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize views
        profilePicture = view.findViewById(R.id.profile_picture);
        usernameText = view.findViewById(R.id.username_text);
        userRatingBar = view.findViewById(R.id.user_rating);
        ratingText = view.findViewById(R.id.rating_text);
        yourListingsButton = view.findViewById(R.id.your_listings_button);
        savedListingsButton = view.findViewById(R.id.saved_listings_button);
        chatHistoryButton = view.findViewById(R.id.chat_history_button);
        toolRequestsButton = view.findViewById(R.id.tool_requests_button);
        publicProfileButton = view.findViewById(R.id.public_profile_button);

        // Set click listeners
        profilePicture.setOnClickListener(v -> {
            if (isAdded()) {
                showProfilePictureOptions();
            }
        });

        yourListingsButton.setOnClickListener(v -> {
            if (isAdded()) {
                YourListingsFragment yourListingsFragment = new YourListingsFragment();
                ((homeActivity) requireActivity()).replaceFragment(yourListingsFragment);
            }
        });

        savedListingsButton.setOnClickListener(v -> {
            if (isAdded()) {
                SavedListingsFragment savedListingsFragment = new SavedListingsFragment();
                ((homeActivity) requireActivity()).replaceFragment(savedListingsFragment);
            }
        });

        chatHistoryButton.setOnClickListener(v -> {
            if (isAdded()) {
                ChatHistoryFragment chatHistoryFragment = new ChatHistoryFragment();
                ((homeActivity) requireActivity()).replaceFragment(chatHistoryFragment);
            }
        });

        toolRequestsButton.setOnClickListener(v -> {
            if (isAdded()) {
                ToolRequestsFragment toolRequestsFragment = new ToolRequestsFragment();
                ((homeActivity) requireActivity()).replaceFragment(toolRequestsFragment);
            }
        });

        publicProfileButton.setOnClickListener(v -> {
            if (isAdded()) {
                PublicProfileFragment publicProfileFragment = PublicProfileFragment.newInstance(currentUser.getUid());
                ((homeActivity) requireActivity()).replaceFragment(publicProfileFragment);
            }
        });

        // Load user profile if logged in
        if (currentUser != null) {
            loadUserProfile();
        } else if (isAdded()) {
            Toast.makeText(requireContext(), "Please log in to view your profile", Toast.LENGTH_SHORT).show();
        }

        return view;
    }

    private void loadUserProfile() {
        String userId = currentUser.getUid();
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
                    }

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
                        userRatingBar.setRating(averageRating);
                        ratingText.setText(String.format("(%.1f)", averageRating));
                    } else {
                        userRatingBar.setRating(0);
                        ratingText.setText("(0.0)");
                    }
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

    private void showProfilePictureOptions() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Profile Picture")
                .setItems(new String[]{"Change Profile Picture"}, (dialog, which) -> {
                    if (which == 0) {
                        photoPicker.launch("image/*");
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void uploadProfilePicture(Uri imageUri) {
        if (currentUser != null && isAdded()) {
            String userId = currentUser.getUid();
            StorageReference profilePicRef = storageReference.child(userId + ".jpg");

            profilePicRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot ->
                            profilePicRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                if (!isAdded()) return;
                                String downloadUrl = uri.toString();
                                usersRef.child(userId).child("profilePictureUrl").setValue(downloadUrl)
                                        .addOnSuccessListener(aVoid -> {
                                            if (isAdded()) {
                                                Picasso.get()
                                                        .load(downloadUrl)
                                                        .placeholder(R.drawable.ic_default_profile)
                                                        .error(R.drawable.ic_default_profile)
                                                        .into(profilePicture);
                                                Toast.makeText(requireContext(),
                                                        "Profile picture updated",
                                                        Toast.LENGTH_SHORT).show();
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            if (isAdded()) {
                                                Toast.makeText(requireContext(),
                                                        "Failed to save profile picture URL: " + e.getMessage(),
                                                        Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }))
                    .addOnFailureListener(e -> {
                        if (isAdded()) {
                            Toast.makeText(requireContext(),
                                    "Failed to upload profile picture: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}