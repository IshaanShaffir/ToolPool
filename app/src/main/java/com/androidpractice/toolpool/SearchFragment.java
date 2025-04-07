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
import android.widget.Toast;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import com.google.android.gms.maps.model.LatLng;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class SearchFragment extends Fragment {

    private LatLng currentSelectedLocation = null;
    // Date format used in the EditText fields, uses device timezone
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public SearchFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // inflate  fragment layout
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        // apply window insets to the root view
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // declare distanceSpinner once for use in the whole method
        Spinner distanceSpinner = view.findViewById(R.id.spinnerDistance);

        EditText locationEditText = view.findViewById(R.id.editTextLocation);
        locationEditText.setFocusable(false);
        locationEditText.setOnClickListener(v -> {
            String distanceStr = distanceSpinner.getSelectedItem().toString(); // get distance string
            // remove " km" and convert to meters
            distanceStr = distanceStr.replace(" km", "").trim();
            double selectedRadiusInMeters = Double.parseDouble(distanceStr) * 1000;

            // create the map fragment with current radius and previously selected location
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
                        // Months are indexed from 0; add 1 for display
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

        // retrieve search text, category, condition, and deposit fields
        EditText searchTextField = view.findViewById(R.id.editTextText);
        Spinner categorySpinner = view.findViewById(R.id.spinnerCategory);
        Spinner conditionSpinner = view.findViewById(R.id.spinnerCondition);
        EditText depositEditText = view.findViewById(R.id.editTextDeposit);
        Button searchButton = view.findViewById(R.id.buttonSearch);

        searchButton.setOnClickListener(v -> {
            String query = searchTextField.getText().toString().trim();
            String selectedCategory = categorySpinner.getSelectedItem().toString().trim();
            String selectedCondition = conditionSpinner.getSelectedItem().toString().trim();

            if (selectedCategory.equalsIgnoreCase("Select Category")) {
                selectedCategory = "";
            }
            if (selectedCondition.equalsIgnoreCase("Select Condition")) {
                selectedCondition = "";
            }

            String distanceStr = distanceSpinner.getSelectedItem().toString().replace(" km", "").trim();
            double radius = Double.parseDouble(distanceStr) * 1000; // convert km to meters

            double lat = currentSelectedLocation != null ? currentSelectedLocation.latitude : 0;
            double lng = currentSelectedLocation != null ? currentSelectedLocation.longitude : 0;
            boolean filterByLocation = currentSelectedLocation != null;

            long lendDateTimestamp = -1;
            long returnDateTimestamp = -1;
            try {
                String lendDateStr = editTextLendDate.getText().toString().trim();
                String returnDateStr = editTextReturnDate.getText().toString().trim();

                if (!lendDateStr.isEmpty()) {
                    Calendar lendCal = Calendar.getInstance();
                    lendCal.setTime(dateFormat.parse(lendDateStr));
                    lendCal.set(Calendar.HOUR_OF_DAY, 0);
                    lendCal.set(Calendar.MINUTE, 0);
                    lendCal.set(Calendar.SECOND, 0);
                    lendCal.set(Calendar.MILLISECOND, 0);
                    lendDateTimestamp = lendCal.getTimeInMillis();
                }
                if (!returnDateStr.isEmpty()) {
                    Calendar returnCal = Calendar.getInstance();
                    returnCal.setTime(dateFormat.parse(returnDateStr));
                    returnCal.set(Calendar.HOUR_OF_DAY, 0);
                    returnCal.set(Calendar.MINUTE, 0);
                    returnCal.set(Calendar.SECOND, 0);
                    returnCal.set(Calendar.MILLISECOND, 0);
                    returnDateTimestamp = returnCal.getTimeInMillis();
                }
            } catch (ParseException e) {
                Toast.makeText(getContext(), "Invalid date format. Please use dd/MM/yyyy", Toast.LENGTH_SHORT).show();
                return;
            }
            if (lendDateTimestamp > 0 && returnDateTimestamp < 0) {
                returnDateTimestamp = lendDateTimestamp;
            } else if (returnDateTimestamp > 0 && lendDateTimestamp < 0) {
                lendDateTimestamp = returnDateTimestamp;
            }
            if (lendDateTimestamp > 0 && returnDateTimestamp > 0 && lendDateTimestamp > returnDateTimestamp) {
                Toast.makeText(getContext(), "Lend date must be before return date", Toast.LENGTH_SHORT).show();
                return;
            }

            double depositFilter = -1;
            String depositStr = depositEditText.getText().toString().trim();
            if (!depositStr.isEmpty()) {
                try {
                    depositFilter = Double.parseDouble(depositStr);
                } catch (NumberFormatException e) {
                    Toast.makeText(getContext(), "Invalid deposit amount", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            if (getActivity() instanceof homeActivity) {
                ((homeActivity) getActivity()).replaceFragment(
                        SearchResultsFragment.newInstance(query, selectedCategory, selectedCondition,
                                filterByLocation, lat, lng, radius, lendDateTimestamp, returnDateTimestamp, depositFilter)
                );
            }
        });
    }
}
