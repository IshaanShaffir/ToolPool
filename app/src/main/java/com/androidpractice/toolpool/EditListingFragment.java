package com.androidpractice.toolpool;

import android.app.DatePickerDialog;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.androidpractice.toolpool.databinding.FragmentEditListingBinding;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EditListingFragment extends Fragment {
    private FragmentEditListingBinding binding;
    private List<Uri> newPhotoUris = new ArrayList<>(); // Local URIs for new photos
    private PhotosAdapter photosAdapter;
    private FirebaseAuth auth;
    private DatabaseReference databaseReference;
    private StorageReference storageReference;
    private Listing listing; // The existing listing being edited

    private Calendar lendDateCalendar = Calendar.getInstance();
    private Calendar returnDateCalendar = Calendar.getInstance();

    private final ActivityResultLauncher<String> photoPicker = registerForActivityResult(
            new ActivityResultContracts.GetMultipleContents(),
            uris -> {
                if (newPhotoUris.size() + uris.size() > 5) {
                    Toast.makeText(getContext(), "Maximum 5 photos", Toast.LENGTH_SHORT).show();
                    return;
                }
                newPhotoUris.addAll(uris);
                photosAdapter.notifyDataSetChanged();
            });

    public EditListingFragment() {}

    // Add the newInstance method here
    public static EditListingFragment newInstance(Listing listing) {
        EditListingFragment fragment = new EditListingFragment();
        Bundle args = new Bundle();
        args.putSerializable("listing", listing);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            listing = (Listing) getArguments().getSerializable("listing");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentEditListingBinding.inflate(inflater, container, false);
        auth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("listings");
        storageReference = FirebaseStorage.getInstance().getReference("listing_photos");

        // Initialize adapter with newPhotoUris (for new photos only)
        photosAdapter = new PhotosAdapter(newPhotoUris);
        binding.photosPreview.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.photosPreview.setAdapter(photosAdapter);

        // Populate fields with existing listing data
        populateFields();

        // Set up date pickers
        setupDatePickers();

        // Set up button listeners
        binding.addPhotosButton.setOnClickListener(v -> photoPicker.launch("image/*"));
        binding.saveButton.setOnClickListener(v -> saveEditedListing());
        binding.deleteButton.setOnClickListener(v -> deleteListing());

        return binding.getRoot();
    }

    private void populateFields() {
        if (listing != null) {
            binding.editTitle.setText(listing.getTitle());
            binding.editDescription.setText(listing.getDescription());
            binding.editCategory.setText(listing.getCategory());
            binding.editAddress.setText(listing.getAddress());
            binding.editDeposit.setText(String.valueOf(listing.getDeposit()));
            binding.editCondition.setText(listing.getCondition());

            // Set dates
            lendDateCalendar.setTimeInMillis(listing.getLendDate());
            returnDateCalendar.setTimeInMillis(listing.getReturnDate());
            updateDateButtonLabels();
        }
    }

    private void setupDatePickers() {
        DatePickerDialog.OnDateSetListener lendDateListener = (view, year, month, dayOfMonth) -> {
            lendDateCalendar.set(Calendar.YEAR, year);
            lendDateCalendar.set(Calendar.MONTH, month);
            lendDateCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateDateButtonLabels();
        };

        DatePickerDialog.OnDateSetListener returnDateListener = (view, year, month, dayOfMonth) -> {
            returnDateCalendar.set(Calendar.YEAR, year);
            returnDateCalendar.set(Calendar.MONTH, month);
            returnDateCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateDateButtonLabels();
        };

        binding.editLendDateButton.setOnClickListener(v -> new DatePickerDialog(
                requireContext(), lendDateListener,
                lendDateCalendar.get(Calendar.YEAR),
                lendDateCalendar.get(Calendar.MONTH),
                lendDateCalendar.get(Calendar.DAY_OF_MONTH)).show());

        binding.editReturnDateButton.setOnClickListener(v -> new DatePickerDialog(
                requireContext(), returnDateListener,
                returnDateCalendar.get(Calendar.YEAR),
                returnDateCalendar.get(Calendar.MONTH),
                returnDateCalendar.get(Calendar.DAY_OF_MONTH)).show());
    }

    private void updateDateButtonLabels() {
        String myFormat = "MM/dd/yyyy";
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);
        binding.editLendDateButton.setText("Lend Date: " + sdf.format(lendDateCalendar.getTime()));
        binding.editReturnDateButton.setText("Return Date: " + sdf.format(returnDateCalendar.getTime()));
    }

    private void saveEditedListing() {
        String title = binding.editTitle.getText().toString().trim();
        String description = binding.editDescription.getText().toString().trim();
        String category = binding.editCategory.getText().toString().trim();
        String address = binding.editAddress.getText().toString().trim();
        String depositStr = binding.editDeposit.getText().toString().trim();
        String condition = binding.editCondition.getText().toString().trim();

        Date lendDate = lendDateCalendar.getTime();
        Date returnDate = returnDateCalendar.getTime();

        if (title.isEmpty() || description.isEmpty() || category.isEmpty() || address.isEmpty() ||
                depositStr.isEmpty() || condition.isEmpty()) {
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
        String listingId = listing.getListingId(); // Use existing listing ID

        geocodeAddress(address, latLng -> {
            // Create updated listing with existing photoUrls
            List<String> photoUrls = listing.getPhotoUrls() != null ? new ArrayList<>(listing.getPhotoUrls()) : new ArrayList<>();
            Listing updatedListing = new Listing(
                    listingId, title, description, category, address, deposit,
                    lendDate.getTime(), returnDate.getTime(), userId, photoUrls,
                    condition, listing.isBooked(), latLng.latitude, latLng.longitude
            );

            binding.saveButton.setEnabled(false);

            // If no new photos are added, save directly with existing photoUrls
            if (newPhotoUris.isEmpty()) {
                saveListing(listingId, updatedListing);
            } else {
                // Upload new photos and append their URLs to the existing photoUrls
                uploadNewPhotos(listingId, updatedListing);
            }
        });
    }

    private void uploadNewPhotos(String listingId, Listing listing) {
        List<String> photoUrls = new ArrayList<>(listing.getPhotoUrls()); // Start with existing URLs
        StorageReference listingFolder = storageReference.child(listingId);
        int startIndex = photoUrls.size(); // Append new photos after existing ones
        int[] completedUploads = {0};

        for (int i = 0; i < newPhotoUris.size(); i++) {
            Uri photoUri = newPhotoUris.get(i);
            StorageReference photoRef = listingFolder.child("photo_" + (startIndex + i) + ".jpg");

            photoRef.putFile(photoUri)
                    .addOnSuccessListener(taskSnapshot ->
                            photoRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                photoUrls.add(uri.toString());
                                completedUploads[0]++;
                                if (completedUploads[0] == newPhotoUris.size()) {
                                    listing.setPhotoUrls(photoUrls);
                                    saveListing(listingId, listing);
                                }
                            }))
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Photo upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        binding.saveButton.setEnabled(true);
                    });
        }
    }

    private void saveListing(String listingId, Listing listing) {
        databaseReference.child(listingId)
                .setValue(listing)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(requireContext(), "Listing updated!", Toast.LENGTH_SHORT).show();
                    requireActivity().getSupportFragmentManager().popBackStack(); // Go back
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    binding.saveButton.setEnabled(true);
                });
    }

    private void deleteListing() {
        String listingId = listing.getListingId();
        databaseReference.child(listingId)
                .removeValue()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(requireContext(), "Listing deleted!", Toast.LENGTH_SHORT).show();
                    requireActivity().getSupportFragmentManager().popBackStack(); // Go back
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Error deleting listing: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void geocodeAddress(String address, OnGeocodeSuccessListener listener) {
        Geocoder geocoder = new Geocoder(requireContext());
        try {
            List<Address> addresses = geocoder.getFromLocationName(address, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address location = addresses.get(0);
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                listener.onSuccess(latLng);
            } else {
                Toast.makeText(getContext(), "Address not found", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Toast.makeText(getContext(), "Geocoding failed", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    interface OnGeocodeSuccessListener {
        void onSuccess(LatLng latLng);
    }
}