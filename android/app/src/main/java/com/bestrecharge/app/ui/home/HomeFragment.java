package com.bestrecharge.app.ui.home;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bestrecharge.app.R;
import com.bestrecharge.app.data.local.entities.SimProfileEntity;
import com.bestrecharge.app.databinding.FragmentHomeBinding;
import com.bestrecharge.app.telephony.SimDetectorManager;
import com.bestrecharge.app.ui.deals.BestDealActivity;
import com.bestrecharge.app.ui.dialogs.ChangeNumberDialog;
import com.bestrecharge.app.ui.main.MainActivity;
import com.bestrecharge.app.ui.ocr.ScreenshotImportActivity;

import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;
    private List<SimProfileEntity> allSims;
    private SimProfileEntity currentSim;
    private String currentPlanId = "vi_349_hero";

    private ActivityResultLauncher<String[]> telephonyPermissionLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        telephonyPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    boolean granted = false;
                    for (Boolean g : result.values()) {
                        if (Boolean.TRUE.equals(g)) {
                            granted = true;
                            break;
                        }
                    }
                    if (granted) {
                        detectDeviceSims();
                    } else {
                        Toast.makeText(getContext(), "SIM permission denied. You can enter any number manually via Change.", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        viewModel.getAllSims().observe(getViewLifecycleOwner(), sims -> {
            allSims = sims;
        });

        viewModel.getPrimarySim().observe(getViewLifecycleOwner(), sim -> {
            if (sim != null) {
                currentSim = sim;
                bindSimDetails(sim);
            }
        });

        binding.btnAutoDetectSim.setOnClickListener(v -> checkAndDetectSim());

        binding.btnChangeNumber.setOnClickListener(v -> {
            if (getContext() != null) {
                new ChangeNumberDialog(getContext(), currentSim, selectedSim -> {
                    currentSim = selectedSim;
                    bindSimDetails(selectedSim);
                    Toast.makeText(getContext(), "Active recharge number updated to " + selectedSim.operatorName, Toast.LENGTH_SHORT).show();
                }).show();
            }
        });

        binding.btnOpenOfficialApp.setOnClickListener(v -> openOfficialTelecomApp());

        binding.btnInspectDeal.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), BestDealActivity.class);
            intent.putExtra("PLAN_ID", currentPlanId);
            startActivity(intent);
        });

        binding.cardTopDeal.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), BestDealActivity.class);
            intent.putExtra("PLAN_ID", currentPlanId);
            startActivity(intent);
        });

        binding.btnOcrImport.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), ScreenshotImportActivity.class);
            startActivity(intent);
        });

        binding.cardActionCompare.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToTab(R.id.navigation_compare);
            }
        });

        binding.cardActionOffers.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToTab(R.id.navigation_offers);
            }
        });

        // Automatically detect device SIM on startup if permission is already granted
        if (getContext() != null && SimDetectorManager.hasTelephonyPermissions(getContext())) {
            detectDeviceSims();
        }
    }

    private void bindSimDetails(SimProfileEntity sim) {
        binding.tvSimSlot.setText(sim.simSlot != null ? sim.simSlot.toUpperCase() : "ACTIVE RECHARGE");
        String rawNum = sim.mobileNumber != null ? sim.mobileNumber.replaceAll("[^0-9]", "") : "";
        String numberDisplay;
        if (rawNum.length() == 10) {
            numberDisplay = "+91 " + rawNum.substring(0, 5) + " " + rawNum.substring(5);
        } else if (!rawNum.isEmpty()) {
            numberDisplay = "+91 " + rawNum;
        } else {
            numberDisplay = "No Number Set (Tap Change)";
        }
        binding.tvMobileNumber.setText(numberDisplay);
        binding.tvOperatorCircle.setText(sim.operatorName + " • " + sim.circleName + (sim.isPrepaid ? " (Prepaid)" : " (Postpaid)"));

        updateTopDealForOperator(sim);
    }

    private void updateTopDealForOperator(SimProfileEntity sim) {
        String op = sim.operatorCode != null ? sim.operatorCode.toUpperCase() : "VI";
        binding.tvTopDealHeading.setText("TOP RECOMMENDED DEAL FOR " + op);
        binding.btnOpenOfficialApp.setText("Check in " + (op.equals("JIO") ? "MyJio App" : op.equals("AIRTEL") ? "Airtel Thanks" : op.equals("BSNL") ? "BSNL Portal" : "Vi App"));

        if ("JIO".equals(op)) {
            currentPlanId = "jio_349_popular";
            binding.tvDealBadgeLabel.setText("TRUE 5G UNLIMITED");
            binding.tvDealSaveAmount.setText("Save ₹100");
            binding.tvDealPlanTitle.setText("Jio True 5G ₹349");
            binding.tvDealBenefits.setText("28 Days • 2GB/day • Truly Unlimited Calls • JioCinema & JioTV");
            binding.tvMrpPrice.setText("₹349");
            binding.tvPayablePrice.setText("₹349");
            binding.tvEffectivePrice.setText("₹249");
            binding.tvDealStackExplanation.setText("Jio True 5G Pack + Paytm Assured ₹100 Cashback via UPI");
        } else if ("AIRTEL".equals(op)) {
            currentPlanId = "airtel_349_popular";
            binding.tvDealBadgeLabel.setText("AIRTEL 5G PLUS");
            binding.tvDealSaveAmount.setText("Save ₹75");
            binding.tvDealPlanTitle.setText("Airtel Unlimited ₹349");
            binding.tvDealBenefits.setText("28 Days • 1.5GB/day • Truly Unlimited Calls • Airtel Xstream Play");
            binding.tvMrpPrice.setText("₹349");
            binding.tvPayablePrice.setText("₹349");
            binding.tvEffectivePrice.setText("₹274");
            binding.tvDealStackExplanation.setText("Airtel 5G Plus + BHIM Payments App up to ₹75 Cashback");
        } else if ("BSNL".equals(op)) {
            currentPlanId = "bsnl_199_value";
            binding.tvDealBadgeLabel.setText("BEST VALUE 30 DAYS");
            binding.tvDealSaveAmount.setText("Save ₹25");
            binding.tvDealPlanTitle.setText("BSNL Value ₹199");
            binding.tvDealBenefits.setText("30 Days • 2GB/day • Unlimited Local & STD Calls");
            binding.tvMrpPrice.setText("₹199");
            binding.tvPayablePrice.setText("₹199");
            binding.tvEffectivePrice.setText("₹174");
            binding.tvDealStackExplanation.setText("Affordable BSNL Tariff + PhonePe Flat ₹25 Cashback");
        } else {
            // Default Vi
            currentPlanId = "vi_349_hero";
            binding.tvDealBadgeLabel.setText("MAX SAVINGS STACK");
            binding.tvDealSaveAmount.setText("Save ₹140");
            binding.tvDealPlanTitle.setText("Vi Hero Unlimited ₹349");
            binding.tvDealBenefits.setText("28 Days • 1.5GB/day • Unlimited Calls • Night Binge (12-6am)");
            binding.tvMrpPrice.setText("₹349");
            binding.tvPayablePrice.setText("₹334");
            binding.tvEffectivePrice.setText("₹209");
            binding.tvDealStackExplanation.setText("Vi App ₹15 Instant Discount + Paytm UPI ₹125 Assured Cashback");
        }
    }

    private void checkAndDetectSim() {
        if (getContext() == null) return;
        if (SimDetectorManager.hasTelephonyPermissions(getContext())) {
            detectDeviceSims();
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                telephonyPermissionLauncher.launch(new String[]{
                        Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.READ_PHONE_NUMBERS
                });
            } else {
                telephonyPermissionLauncher.launch(new String[]{
                        Manifest.permission.READ_PHONE_STATE
                });
            }
        }
    }

    private void detectDeviceSims() {
        if (getContext() == null) return;
        Toast.makeText(getContext(), "Reading hardware SIM cards...", Toast.LENGTH_SHORT).show();

        SimDetectorManager.detectSimCards(getContext(), new SimDetectorManager.OnSimDetectedListener() {
            @Override
            public void onSimsDetected(List<SimProfileEntity> detectedSims) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    if (detectedSims != null && !detectedSims.isEmpty()) {
                        SimProfileEntity primary = detectedSims.get(0);
                        currentSim = primary;
                        bindSimDetails(primary);

                        if (primary.mobileNumber == null || primary.mobileNumber.isEmpty()) {
                            Toast.makeText(getContext(), "Detected " + primary.operatorName + " (" + primary.simSlot + "). Tap 'Change' to set 10-digit number.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getContext(), "Auto-detected: " + primary.operatorName + " " + primary.mobileNumber, Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "No active SIM card detected in device slots.", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onPermissionNeeded() {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Telephony permission needed to read hardware SIM card.", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String message) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void openOfficialTelecomApp() {
        if (currentSim == null || getContext() == null) return;
        String op = currentSim.operatorCode != null ? currentSim.operatorCode.toUpperCase() : "VI";

        String packageName = "com.mventus.selfcare.activity"; // Vi App
        String webUrl = "https://www.myvi.in/prepaid/best-prepaid-plans";

        if ("JIO".equals(op)) {
            packageName = "com.jio.myjio";
            webUrl = "https://www.jio.com/selfcare/plans/mobility/prepaid-plans-list/";
        } else if ("AIRTEL".equals(op)) {
            packageName = "com.myairtelapp";
            webUrl = "https://www.airtel.in/recharge-online";
        } else if ("BSNL".equals(op)) {
            packageName = "in.co.bsnl";
            webUrl = "https://portal2.bsnl.in/myportal/tariffs.do";
        }

        try {
            Intent intent = getContext().getPackageManager().getLaunchIntentForPackage(packageName);
            if (intent != null) {
                startActivity(intent);
                return;
            }
        } catch (Exception ignored) {}

        // Fallback to official web portal
        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUrl));
            startActivity(browserIntent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Cannot open official portal", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
