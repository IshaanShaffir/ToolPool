package com.androidpractice.toolpool;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.DatePicker;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Calendar;

public class searchActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_search);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Lend Date Picker
        EditText editTextLendDate = findViewById(R.id.editTextLendDate);
        editTextLendDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(searchActivity.this,
                    new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(DatePicker view, int selectedYear, int selectedMonth, int selectedDay) {
                            // Months are indexed from 0, so add 1 for display.
                            editTextLendDate.setText(selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear);
                        }
                    }, year, month, day);
            datePickerDialog.show();
        });

        // Return Date Picker
        EditText editTextReturnDate = findViewById(R.id.editTextReturnDate);
        editTextReturnDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(searchActivity.this,
                    new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(DatePicker view, int selectedYear, int selectedMonth, int selectedDay) {
                            editTextReturnDate.setText(selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear);
                        }
                    }, year, month, day);
            datePickerDialog.show();
        });

        // Setup Bottom Navigation View
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        // Set the "search" item as selected.
        bottomNavigationView.setSelectedItemId(R.id.search);
        bottomNavigationView.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == R.id.home) {
                    startActivity(new Intent(searchActivity.this, homeActivity.class));
                    overridePendingTransition(0, 0);
                    finish();
                    return true;
                }
                // If search is selected, do nothing.
                return itemId == R.id.search;
            }
        });
    }
}
