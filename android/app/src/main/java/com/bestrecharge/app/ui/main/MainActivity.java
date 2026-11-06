package com.bestrecharge.app.ui.main;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
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

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(insets.left, insets.top, insets.right, 0);
            return windowInsets;
        });

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

    @Override
    protected void onResume() {
        super.onResume();
        if (com.bestrecharge.app.BestRechargeApp.pendingCelebrationRecord != null) {
            com.bestrecharge.app.data.local.entities.SavingsRecordEntity rec = com.bestrecharge.app.BestRechargeApp.pendingCelebrationRecord;
            com.bestrecharge.app.BestRechargeApp.pendingCelebrationRecord = null;
            int totalSaved = (int) (rec.originalMrp - rec.effectiveCost);
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("🎉 Recharge Completed!")
                    .setMessage("Congratulations! You saved ₹" + totalSaved + " on your recharge for +91 " + rec.mobileNumber + "!\n\n" +
                            "• Original MRP: ₹" + (int) rec.originalMrp + "\n" +
                            "• Amount Paid: ₹" + (int) rec.amountPaid + "\n" +
                            "• Final Effective Cost: ₹" + (int) rec.effectiveCost + "\n" +
                            "• Assured Savings: ₹" + totalSaved + "\n\n" +
                            "Thank you for recharging smartly with BestRecharge! Your transaction is logged in your Savings Tracker.")
                    .setPositiveButton("View in Savings", (d, w) -> navigateToTab(R.id.navigation_savings))
                    .setNegativeButton("Done", null)
                    .setCancelable(false)
                    .show();
        }
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
