package com.androidpractice.toolpool;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.androidpractice.toolpool.databinding.FragmentCreateListingBinding;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class CreateListingFragment extends Fragment {
    private FragmentCreateListingBinding binding;
    private List<Uri> photoUris = new ArrayList<>(); // For preview only
    private PhotosAdapter photosAdapter;
    private FirebaseAuth auth;
    private DatabaseReference databaseReference;

    private final ActivityResultLauncher<String> photoPicker = registerForActivityResult(
            new ActivityResultContracts.GetMultipleContents(),
            uris -> {
                photoUris.addAll(uris);
                photosAdapter.notifyDataSetChanged();
            });

    public CreateListingFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCreateListingBinding.inflate(inflater, container, false);
        auth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("listings");

        // Setup RecyclerView for photo preview
        photosAdapter = new PhotosAdapter(photoUris);
        binding.photosPreview.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.photosPreview.setAdapter(photosAdapter);

        // Setup category spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"example1", "example2", "example3"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.categorySpinner.setAdapter(adapter);

        // Set minimum date for lend date picker to today
        binding.lendDatePicker.setMinDate(System.currentTimeMillis() - 1000);

        // Add photos button
        binding.addPhotosButton.setOnClickListener(v -> photoPicker.launch("image/*"));

        // Create listing button
        binding.createListingButton.setOnClickListener(v -> createListing());

        return binding.getRoot();
    }

    private void createListing() {
        String title = binding.titleInput.getText().toString().trim();
        String description = binding.descriptionInput.getText().toString().trim();
        String category = binding.categorySpinner.getSelectedItem().toString();
        String address = binding.addressInput.getText().toString().trim();
        String depositStr = binding.depositInput.getText().toString().trim();

        // Get dates from DatePickers
        Calendar lendCalendar = Calendar.getInstance();
        lendCalendar.set(
                binding.lendDatePicker.getYear(),
                binding.lendDatePicker.getMonth(),
                binding.lendDatePicker.getDayOfMonth()
        );
        Date lendDate = lendCalendar.getTime();

        Calendar returnCalendar = Calendar.getInstance();
        returnCalendar.set(
                binding.returnDatePicker.getYear(),
                binding.returnDatePicker.getMonth(),
                binding.returnDatePicker.getDayOfMonth()
        );
        Date returnDate = returnCalendar.getTime();

        // Validation
        if (title.isEmpty() || description.isEmpty() || address.isEmpty() || depositStr.isEmpty()) {
            Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (returnDate.before(lendDate)) {
            Toast.makeText(getContext(), "Return date must be after lend date", Toast.LENGTH_SHORT).show();
            return;
        }

        double deposit;
        try {
            deposit = Double.parseDouble(depositStr);
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Invalid deposit amount", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create listing object (no photos)
        String userId = auth.getCurrentUser().getUid();
        Listing listing = new Listing(
                title,
                description,
                category,
                address,
                deposit,
                userId,
                lendDate.getTime(),  // Store as timestamp
                returnDate.getTime() // Store as timestamp
        );

        // Save to Firebase
        String listingId = databaseReference.push().getKey();
        listing.setListingId(listingId);

        databaseReference.child(listingId).setValue(listing)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Listing created successfully", Toast.LENGTH_SHORT).show();
                    photoUris.clear(); // Clear preview photos
                    photosAdapter.notifyDataSetChanged();
                    // Navigate back to HomeFragment
                    if (getActivity() instanceof homeActivity) {
                        ((homeActivity) getActivity()).replaceFragment(new HomeFragment());
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to create listing: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}