package com.androidpractice.toolpool;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.androidpractice.toolpool.databinding.ActivityHomeBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.androidpractice.toolpool.R;

public class homeActivity extends AppCompatActivity {
    ActivityHomeBinding binding;
    FirebaseAuth auth;
    FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        FirebaseApp.initializeApp(this);
        super.onCreate(savedInstanceState);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        if (user == null) {
            Intent intent = new Intent(getApplicationContext(), Login.class);
            startActivity(intent);
            finish();
            return;
        }

        // Add click listener for the add listing button
        binding.addListingButton.setOnClickListener(v -> {
            replaceFragment(new CreateListingFragment());
        });

        replaceFragment(new HomeFragment());
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_home) {
                replaceFragment(new HomeFragment());
            } else if (item.getItemId() == R.id.nav_search) {
                replaceFragment(new SearchFragment());
            } else if (item.getItemId() == R.id.nav_profile) {
                replaceFragment(new ProfileFragment());
            } else if (item.getItemId() == R.id.nav_map) {
                replaceFragment(new MapFragment());
            } else if (item.getItemId() == R.id.nav_settings) {
                replaceFragment(new SettingsFragment());
            }
            return true;
        });
    }

    void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        // Add to back stack only if it's not one of the main navigation fragments
        Fragment currentFragment = fragmentManager.findFragmentById(R.id.fragment_container);
        if (!(fragment instanceof HomeFragment || fragment instanceof SearchFragment ||
                fragment instanceof ProfileFragment || fragment instanceof MapFragment ||
                fragment instanceof SettingsFragment)) {
            fragmentTransaction.addToBackStack(null);
        }
        fragmentTransaction.commit();
    }

    @Override
    public void onBackPressed() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager.getBackStackEntryCount() > 0) {
            // Pop the top fragment from the back stack
            fragmentManager.popBackStack();
        } else {
            // If no fragments in back stack, check current fragment and return to Home if not already there
            Fragment currentFragment = fragmentManager.findFragmentById(R.id.fragment_container);
            if (!(currentFragment instanceof HomeFragment)) {
                replaceFragment(new HomeFragment());
                binding.bottomNavigation.setSelectedItemId(R.id.nav_home);
            } else {
                // If on HomeFragment with no back stack, exit the app
                super.onBackPressed();
            }
        }
    }
}