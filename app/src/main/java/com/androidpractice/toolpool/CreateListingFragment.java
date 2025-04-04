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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.androidpractice.toolpool.databinding.FragmentCreateListingBinding;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class CreateListingFragment extends Fragment {
    private FragmentCreateListingBinding binding;
    private List<Uri> photoUris = new ArrayList<>();
    private PhotosAdapter photosAdapter;
    private FirebaseAuth auth;
    private DatabaseReference databaseReference;
    private StorageReference storageReference;

    private final ActivityResultLauncher<String> photoPicker = registerForActivityResult(
            new ActivityResultContracts.GetMultipleContents(),
            uris -> {
                if (photoUris.size() + uris.size() > 5) {
                    Toast.makeText(getContext(), "Maximum 5 photos", Toast.LENGTH_SHORT).show();
                    return;
                }
                photoUris.addAll(uris);
                photosAdapter.notifyDataSetChanged();
            });

    public CreateListingFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCreateListingBinding.inflate(inflater, container, false);
        auth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("listings");
        storageReference = FirebaseStorage.getInstance().getReference("listing_photos");

        photosAdapter = new PhotosAdapter(photoUris);
        binding.photosPreview.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.photosPreview.setAdapter(photosAdapter);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"example1", "example2", "example3"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.categorySpinner.setAdapter(adapter);

        binding.lendDatePicker.setMinDate(System.currentTimeMillis() - 1000);
        binding.addPhotosButton.setOnClickListener(v -> photoPicker.launch("image/*"));
        binding.createListingButton.setOnClickListener(v -> createListing());

        return binding.getRoot();
    }

    private void createListing() {
        String title = binding.titleInput.getText().toString().trim();
        String description = binding.descriptionInput.getText().toString().trim();
        String category = binding.categorySpinner.getSelectedItem().toString();
        String address = binding.addressInput.getText().toString().trim();
        String depositStr = binding.depositInput.getText().toString().trim();

        Calendar lendCalendar = Calendar.getInstance();
        lendCalendar.set(binding.lendDatePicker.getYear(),
                binding.lendDatePicker.getMonth(),
                binding.lendDatePicker.getDayOfMonth());
        Date lendDate = lendCalendar.getTime();

        Calendar returnCalendar = Calendar.getInstance();
        returnCalendar.set(binding.returnDatePicker.getYear(),
                binding.returnDatePicker.getMonth(),
                binding.returnDatePicker.getDayOfMonth());
        Date returnDate = returnCalendar.getTime();

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

        String userId = auth.getCurrentUser().getUid();
        String listingId = databaseReference.push().getKey();
        Listing listing = new Listing(title, description, category, address,
                deposit, userId, lendDate.getTime(), returnDate.getTime());
        listing.setListingId(listingId);

        binding.createListingButton.setEnabled(false); // Prevent multiple clicks
        if (photoUris.isEmpty()) {
            saveListing(listingId, listing);
        } else {
            uploadPhotos(listingId, listing);
        }
    }

    private void uploadPhotos(String listingId, Listing listing) {
        List<String> photoUrls = new ArrayList<>();
        StorageReference listingFolder = storageReference.child(listingId);
        int[] completedUploads = {0};

        for (int i = 0; i < photoUris.size(); i++) {
            Uri photoUri = photoUris.get(i);
            StorageReference photoRef = listingFolder.child("photo_" + i + ".jpg");

            photoRef.putFile(photoUri)
                    .addOnSuccessListener(taskSnapshot ->
                            photoRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                photoUrls.add(uri.toString());
                                completedUploads[0]++;
                                if (completedUploads[0] == photoUris.size()) {
                                    listing.setPhotoUrls(photoUrls);
                                    saveListing(listingId, listing);
                                }
                            }))
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Photo upload failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        binding.createListingButton.setEnabled(true);
                    });
        }
    }

    private void saveListing(String listingId, Listing listing) {
        databaseReference.child(listingId).setValue(listing)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Listing created successfully", Toast.LENGTH_SHORT).show();
                    photoUris.clear();
                    photosAdapter.notifyDataSetChanged();
                    binding.createListingButton.setEnabled(true);
                    if (getActivity() instanceof homeActivity) {
                        ((homeActivity) getActivity()).replaceFragment(new HomeFragment());
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to create listing: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    binding.createListingButton.setEnabled(true);
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}