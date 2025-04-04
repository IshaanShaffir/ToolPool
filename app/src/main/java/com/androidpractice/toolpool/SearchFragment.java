package com.androidpractice.toolpool;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import com.google.android.gms.maps.model.LatLng;
import java.util.Calendar;

public class SearchFragment extends Fragment {

    // Store the currently selected location to be passed back to the map dialog.
    private LatLng currentSelectedLocation = null;

    public SearchFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the fragment layout.
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        // Apply window insets to the root view.
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        EditText locationEditText = view.findViewById(R.id.editTextLocation);
        Spinner distanceSpinner = view.findViewById(R.id.spinner); // Ensure spinner id matches layout
        locationEditText.setFocusable(false);
        locationEditText.setOnClickListener(v -> {
            // Retrieve the selected distance string (e.g., "25 km")
            String distanceStr = distanceSpinner.getSelectedItem().toString();
            // Remove " km" and convert to meters.
            distanceStr = distanceStr.replace(" km", "").trim();
            double selectedRadiusInMeters = Double.parseDouble(distanceStr) * 1000;

            // Create the MapDialogFragment with the current radius and previously selected location (if any)
            MapDialogFragment mapDialog = MapDialogFragment.newInstance(selectedRadiusInMeters, currentSelectedLocation);
            mapDialog.setOnLocationSelectedListener((latLng, address) -> {
                locationEditText.setText(address);
                currentSelectedLocation = latLng;
            });
            mapDialog.show(getChildFragmentManager(), "mapDialog");
        });

        // Lend Date Picker
        EditText editTextLendDate = view.findViewById(R.id.editTextLendDate);
        editTextLendDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                    (DatePicker dp, int selectedYear, int selectedMonth, int selectedDay) -> {
                        // Months are indexed from 0; add 1 for display.
                        editTextLendDate.setText(selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear);
                    }, year, month, day);
            datePickerDialog.show();
        });

        // Return Date Picker
        EditText editTextReturnDate = view.findViewById(R.id.editTextReturnDate);
        editTextReturnDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                    (DatePicker dp, int selectedYear, int selectedMonth, int selectedDay) -> {
                        editTextReturnDate.setText(selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear);
                    }, year, month, day);
            datePickerDialog.show();
        });

        // search functionality
        EditText searchTextField = view.findViewById(R.id.editTextText);
        Button searchButton = view.findViewById(R.id.buttonSearch);
        searchButton.setOnClickListener(v -> {
            String query = searchTextField.getText().toString().trim();
            if (getActivity() instanceof homeActivity) {
                // Use the newInstance method to pass the search query to SearchResultsFragment.
                ((homeActivity) getActivity()).replaceFragment(SearchResultsFragment.newInstance(query));
            }
        });


    }
}
