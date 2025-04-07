package com.androidpractice.toolpool;

import android.app.DatePickerDialog;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
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
import com.androidpractice.toolpool.databinding.FragmentCreateListingBinding;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CreateListingFragment extends Fragment {
    private FragmentCreateListingBinding binding;
    private List<Uri> photoUris = new ArrayList<>();
    private PhotosAdapter photosAdapter;
    private FirebaseAuth auth;
    private DatabaseReference databaseReference;
    private StorageReference storageReference;

    private EditText editTextLendDate, editTextReturnDate;
    private Calendar lendDateCalendar = Calendar.getInstance();
    private Calendar returnDateCalendar = Calendar.getInstance();

    interface OnGeocodeSuccessListener {
        void onSuccess(LatLng latLng);
    }

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
                new String[]{"Power Tools", "Hand Tools", "Garden Tools", "Automotive", "Other"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.categorySpinner.setAdapter(adapter);

        editTextLendDate = binding.editTextLendDate;
        editTextReturnDate = binding.editTextReturnDate;
        setupDatePickers();

        binding.addPhotosButton.setOnClickListener(v -> photoPicker.launch("image/*"));
        binding.createListingButton.setOnClickListener(v -> createListing());

        return binding.getRoot();
    }

    private void setupDatePickers() {
        DatePickerDialog.OnDateSetListener lendDateListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                lendDateCalendar.set(Calendar.YEAR, year);
                lendDateCalendar.set(Calendar.MONTH, month);
                lendDateCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateLendDateLabel();
            }
        };

        DatePickerDialog.OnDateSetListener returnDateListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                returnDateCalendar.set(Calendar.YEAR, year);
                returnDateCalendar.set(Calendar.MONTH, month);
                returnDateCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateReturnDateLabel();
            }
        };

        editTextLendDate.setOnClickListener(v -> new DatePickerDialog(
                requireContext(),
                lendDateListener,
                lendDateCalendar.get(Calendar.YEAR),
                lendDateCalendar.get(Calendar.MONTH),
                lendDateCalendar.get(Calendar.DAY_OF_MONTH))
                .show());

        editTextReturnDate.setOnClickListener(v -> new DatePickerDialog(
                requireContext(),
                returnDateListener,
                returnDateCalendar.get(Calendar.YEAR),
                returnDateCalendar.get(Calendar.MONTH),
                returnDateCalendar.get(Calendar.DAY_OF_MONTH))
                .show());
    }

    private void updateLendDateLabel() {
        String myFormat = "MM/dd/yyyy";
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);
        editTextLendDate.setText(sdf.format(lendDateCalendar.getTime()));
    }

    private void updateReturnDateLabel() {
        String myFormat = "MM/dd/yyyy";
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);
        editTextReturnDate.setText(sdf.format(returnDateCalendar.getTime()));
    }

    private String getSelectedCondition() {
        int selectedId = binding.conditionGroup.getCheckedRadioButtonId();

        if (selectedId == R.id.condition_new) {
            return "New";
        } else if (selectedId == R.id.condition_good) {
            return "Good";
        } else if (selectedId == R.id.condition_used) {
            return "Used";
        } else if (selectedId == R.id.condition_slight_damage) {
            return "Slight Damage";
        } else {
            return "Unknown";
        }
    }

    private void createListing() {
        String title = binding.titleInput.getText().toString().trim();
        String description = binding.descriptionInput.getText().toString().trim();
        String category = binding.categorySpinner.getSelectedItem().toString();
        String address = binding.addressInput.getText().toString().trim();
        String depositStr = binding.depositInput.getText().toString().trim();

        Date lendDate = lendDateCalendar.getTime();
        Date returnDate = returnDateCalendar.getTime();

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
        String condition = getSelectedCondition();

        geocodeAddress(address, new OnGeocodeSuccessListener() {
            @Override
            public void onSuccess(LatLng latLng) {
                Listing listing = new Listing(
                        title, description, category, address,
                        deposit, userId,
                        lendDate.getTime(), returnDate.getTime(),
                        condition,
                        latLng.latitude,
                        latLng.longitude
                );
                listing.setListingId(listingId);

                binding.createListingButton.setEnabled(false);

                if (photoUris.isEmpty()) {
                    saveListing(listingId, listing);
                } else {
                    uploadPhotos(listingId, listing);
                }
            }
        });
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

    private void saveListing(String listingId, Listing listing) {
        databaseReference.child(listingId)
                .setValue(listing)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Toast.makeText(requireContext(), "Listing created!", Toast.LENGTH_SHORT).show();
                        ((homeActivity) requireActivity()).replaceFragment(new HomeFragment());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        binding.createListingButton.setEnabled(true);
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}