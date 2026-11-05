package com.bestrecharge.app.ui.main;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.bestrecharge.app.R;
import com.bestrecharge.app.databinding.ActivityMainBinding;
import com.bestrecharge.app.ui.compare.CompareFragment;
import com.bestrecharge.app.ui.home.HomeFragment;
import com.bestrecharge.app.ui.offers.OffersFragment;
import com.bestrecharge.app.ui.profile.ProfileFragment;
import com.bestrecharge.app.ui.savings.SavingsFragment;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_home) {
                loadFragment(new HomeFragment());
                return true;
            } else if (itemId == R.id.navigation_compare) {
                loadFragment(new CompareFragment());
                return true;
            } else if (itemId == R.id.navigation_offers) {
                loadFragment(new OffersFragment());
                return true;
            } else if (itemId == R.id.navigation_savings) {
                loadFragment(new SavingsFragment());
                return true;
            } else if (itemId == R.id.navigation_profile) {
                loadFragment(new ProfileFragment());
                return true;
            }
            return false;
        });
    }

    public void navigateToTab(int menuItemId) {
        binding.bottomNavigation.setSelectedItemId(menuItemId);
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}
