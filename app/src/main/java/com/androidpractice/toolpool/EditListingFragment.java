package com.androidpractice.toolpool;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class EditListingFragment extends Fragment {
    private static final String ARG_LISTING = "listing";
    private Listing listing;
    private DatabaseReference listingsRef;
    private StorageReference storageReference;

    private EditText editTitle, editDescription, editCategory, editAddress, editDeposit, editCondition;
    private Button editLendDateButton, editReturnDateButton, addPhotosButton, saveButton, deleteButton;
    private RecyclerView photosPreview;
    private long lendDate, returnDate;
    private List<Uri> photoUris = new ArrayList<>();
    private PhotosAdapter photosAdapter;

    private final ActivityResultLauncher<String> photoPicker = registerForActivityResult(
            new ActivityResultContracts.GetMultipleContents(),
            uris -> {
                if (photoUris.size() + uris.size() > 5) {
                    Toast.makeText(requireContext(), "Maximum 5 photos", Toast.LENGTH_SHORT).show();
                    return;
                }
                photoUris.clear(); // Replace existing photos
                photoUris.addAll(uris);
                photosAdapter.notifyDataSetChanged();
            });

    public EditListingFragment() {}

    public static EditListingFragment newInstance(Listing listing) {
        EditListingFragment fragment = new EditListingFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_LISTING, listing);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            listing = (Listing) getArguments().getSerializable(ARG_LISTING);
        }
        listingsRef = FirebaseDatabase.getInstance().getReference("listings");
        storageReference = FirebaseStorage.getInstance().getReference("listing_photos");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_listing, container, false);

        editTitle = view.findViewById(R.id.edit_title);
        editDescription = view.findViewById(R.id.edit_description);
        editCategory = view.findViewById(R.id.edit_category);
        editAddress = view.findViewById(R.id.edit_address);
        editDeposit = view.findViewById(R.id.edit_deposit);
        editCondition = view.findViewById(R.id.edit_condition);
        editLendDateButton = view.findViewById(R.id.edit_lend_date_button);
        editReturnDateButton = view.findViewById(R.id.edit_return_date_button);
        addPhotosButton = view.findViewById(R.id.add_photos_button);
        photosPreview = view.findViewById(R.id.photos_preview);
        saveButton = view.findViewById(R.id.save_button);
        deleteButton = view.findViewById(R.id.delete_button);

        photosAdapter = new PhotosAdapter(photoUris);
        photosPreview.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        photosPreview.setAdapter(photosAdapter);

        if (listing != null) {
            editTitle.setText(listing.getTitle());
            editDescription.setText(listing.getDescription());
            editCategory.setText(listing.getCategory());
            editAddress.setText(listing.getAddress());
            editDeposit.setText(String.valueOf(listing.getDeposit()));
            editCondition.setText(listing.getCondition());
            lendDate = listing.getLendDate();
            returnDate = listing.getReturnDate();
            if (listing.getPhotoUrls() != null && !listing.getPhotoUrls().isEmpty()) {
                for (String url : listing.getPhotoUrls()) {
                    photoUris.add(Uri.parse(url));
                }
                photosAdapter.notifyDataSetChanged();
            }
            updateDateButtonText();
        }

        editLendDateButton.setOnClickListener(v -> showDatePicker(true));
        editReturnDateButton.setOnClickListener(v -> showDatePicker(false));
        addPhotosButton.setOnClickListener(v -> photoPicker.launch("image/*"));
        saveButton.setOnClickListener(v -> saveChanges());
        deleteButton.setOnClickListener(v -> deleteListing());

        return view;
    }

    private void showDatePicker(boolean isLendDate) {
        Calendar calendar = Calendar.getInstance();
        long currentDate = isLendDate ? lendDate : returnDate;
        if (currentDate > 0) {
            calendar.setTimeInMillis(currentDate);
        }
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    Calendar selectedCalendar = Calendar.getInstance();
                    selectedCalendar.set(selectedYear, selectedMonth, selectedDay);
                    if (isLendDate) {
                        lendDate = selectedCalendar.getTimeInMillis();
                    } else {
                        returnDate = selectedCalendar.getTimeInMillis();
                    }
                    updateDateButtonText();
                },
                year, month, day);
        datePickerDialog.show();
    }

    private void updateDateButtonText() {
        Calendar calendar = Calendar.getInstance();
        if (lendDate > 0) {
            calendar.setTimeInMillis(lendDate);
            editLendDateButton.setText("Lend Date: " + (calendar.get(Calendar.MONTH) + 1) + "/" +
                    calendar.get(Calendar.DAY_OF_MONTH) + "/" + calendar.get(Calendar.YEAR));
        }
        if (returnDate > 0) {
            calendar.setTimeInMillis(returnDate);
            editReturnDateButton.setText("Return Date: " + (calendar.get(Calendar.MONTH) + 1) + "/" +
                    calendar.get(Calendar.DAY_OF_MONTH) + "/" + calendar.get(Calendar.YEAR));
        }
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
                Toast.makeText(requireContext(), "Address not found", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Toast.makeText(requireContext(), "Geocoding failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    interface OnGeocodeSuccessListener {
        void onSuccess(LatLng latLng);
    }

    private void saveChanges() {
        String title = editTitle.getText().toString().trim();
        String description = editDescription.getText().toString().trim();
        String category = editCategory.getText().toString().trim();
        String address = editAddress.getText().toString().trim();
        String depositStr = editDeposit.getText().toString().trim();
        String condition = editCondition.getText().toString().trim();

        if (title.isEmpty() || description.isEmpty() || category.isEmpty() || address.isEmpty() ||
                depositStr.isEmpty() || condition.isEmpty()) {
            Toast.makeText(requireContext(), "All fields must be filled", Toast.LENGTH_SHORT).show();
            return;
        }

        if (lendDate <= 0 || returnDate <= 0) {
            Toast.makeText(requireContext(), "Please set both lend and return dates", Toast.LENGTH_SHORT).show();
            return;
        }

        if (returnDate <= lendDate) {
            Toast.makeText(requireContext(), "Return date must be after lend date", Toast.LENGTH_SHORT).show();
            return;
        }

        double deposit;
        try {
            deposit = Double.parseDouble(depositStr);
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Invalid deposit amount", Toast.LENGTH_SHORT).show();
            return;
        }

        geocodeAddress(address, latLng -> {
            Listing updatedListing = new Listing(
                    title, description, category, address,
                    deposit, listing.getUserId(),
                    lendDate, returnDate,
                    condition,
                    latLng.latitude, latLng.longitude
            );
            updatedListing.setListingId(listing.getListingId());

            if (photoUris.isEmpty()) {
                updatedListing.setPhotoUrls(listing.getPhotoUrls());
                saveListing(updatedListing);
            } else {
                uploadPhotos(updatedListing);
            }
        });
    }

    private void uploadPhotos(Listing listing) {
        List<String> photoUrls = new ArrayList<>();
        StorageReference listingFolder = storageReference.child(listing.getListingId());
        int[] completedUploads = {0};

        listingFolder.listAll().addOnSuccessListener(listResult -> {
            for (StorageReference item : listResult.getItems()) {
                item.delete();
            }
        });

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
                                    saveListing(listing);
                                }
                            }))
                    .addOnFailureListener(e -> {
                        Toast.makeText(requireContext(), "Photo upload failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void saveListing(Listing listing) {
        listingsRef.child(listing.getListingId()).setValue(listing)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), "Listing updated successfully", Toast.LENGTH_SHORT).show();
                    requireActivity().getSupportFragmentManager().popBackStack();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Failed to update listing: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteListing() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Listing")
                .setMessage("Are you sure you want to delete this listing? This cannot be undone.")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Proceed with deletion
                    if (listing == null || listing.getListingId() == null) {
                        Toast.makeText(requireContext(), "No listing to delete", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    saveButton.setEnabled(false);
                    deleteButton.setEnabled(false);
                    StorageReference listingFolder = storageReference.child(listing.getListingId());
                    listingFolder.listAll()
                            .addOnSuccessListener(listResult -> {
                                if (listResult.getItems().isEmpty()) {
                                    deleteFromDatabase();
                                } else {
                                    int[] deletedCount = {0};
                                    for (StorageReference item : listResult.getItems()) {
                                        item.delete()
                                                .addOnSuccessListener(aVoid -> {
                                                    deletedCount[0]++;
                                                    if (deletedCount[0] == listResult.getItems().size()) {
                                                        deleteFromDatabase();
                                                    }
                                                })
                                                .addOnFailureListener(e -> {
                                                    Toast.makeText(requireContext(), "Failed to delete photo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                    saveButton.setEnabled(true);
                                                    deleteButton.setEnabled(true);
                                                });
                                    }
                                }
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(requireContext(), "Failed to access photos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                saveButton.setEnabled(true);
                                deleteButton.setEnabled(true);
                            });
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void deleteFromDatabase() {
        listingsRef.child(listing.getListingId())
                .removeValue()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), "Listing deleted successfully", Toast.LENGTH_SHORT).show();
                    requireActivity().getSupportFragmentManager().popBackStack(); // Return to previous fragment
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Failed to delete listing: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    saveButton.setEnabled(true);
                    deleteButton.setEnabled(true);
                });
    }
}