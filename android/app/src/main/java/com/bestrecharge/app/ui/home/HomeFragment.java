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
import com.bestrecharge.app.utils.TelecomRedirectionHelper;

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

        binding.btnProviderHandsetAction.setOnClickListener(v -> handleProviderHandsetAction());

        binding.chipGroupPreferences.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            handlePreferenceChanged(checkedIds.get(0));
        });

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

        updateProviderHandsetStatus();
        updateTopDealForOperator(sim);
    }

    private void updateProviderHandsetStatus() {
        if (currentSim == null || getContext() == null) return;
        String op = currentSim.operatorCode != null ? currentSim.operatorCode.toUpperCase() : "VI";
        String opName = TelecomRedirectionHelper.getOperatorDisplayName(op);
        boolean installed = TelecomRedirectionHelper.isProviderAppInstalled(getContext(), op);

        if (installed) {
            binding.tvProviderHandsetStatus.setText("● " + opName + " detected on handset • Ready for live sync");
            binding.tvProviderHandsetStatus.setTextColor(getResources().getColor(R.color.savings_green_dark, null));
            binding.btnProviderHandsetAction.setText("Sync Offers");
        } else {
            binding.tvProviderHandsetStatus.setText("⚠️ " + opName + " not found on handset • Install for ₹15 off");
            binding.tvProviderHandsetStatus.setTextColor(getResources().getColor(R.color.on_surface_muted, null));
            binding.btnProviderHandsetAction.setText("Install App");
        }
    }

    private void handleProviderHandsetAction() {
        if (currentSim == null || getContext() == null) return;
        String op = currentSim.operatorCode != null ? currentSim.operatorCode.toUpperCase() : "VI";
        boolean installed = TelecomRedirectionHelper.isProviderAppInstalled(getContext(), op);

        if (installed) {
            triggerProcessingAnimation("Surfing live app-exclusive coupons and payment cashback for " + TelecomRedirectionHelper.getOperatorDisplayName(op) + "...");
        } else {
            String pkg = TelecomRedirectionHelper.getPackageForOperator(op);
            TelecomRedirectionHelper.openPlayStore(getContext(), pkg);
        }
    }

    private void handlePreferenceChanged(int chipId) {
        if (currentSim == null) return;
        String prefLabel = "all best deals";
        if (chipId == R.id.chip_pref_under_300) {
            prefLabel = "plans under ₹300";
        } else if (chipId == R.id.chip_pref_1_5_gb) {
            prefLabel = "1.5 GB/day plans";
        } else if (chipId == R.id.chip_pref_1_gb) {
            prefLabel = "1 GB/day plans";
        } else if (chipId == R.id.chip_pref_validity) {
            prefLabel = "service validity only";
        } else if (chipId == R.id.chip_pref_long_term) {
            prefLabel = "84+ days long term plans";
        }

        triggerProcessingAnimation("Autonomously comparing " + prefLabel + " with app discounts & UPI cashback...");
    }

    private void triggerProcessingAnimation(String statusText) {
        binding.cardProcessingState.setVisibility(View.VISIBLE);
        binding.tvProcessingStatus.setText(statusText);

        binding.cardProcessingState.postDelayed(() -> {
            if (binding != null) {
                binding.cardProcessingState.setVisibility(View.GONE);
                updateTopDealForOperator(currentSim);
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Smart deal recalculated!", Toast.LENGTH_SHORT).show();
                }
            }
        }, 800);
    }

    private void updateTopDealForOperator(SimProfileEntity sim) {
        String op = sim.operatorCode != null ? sim.operatorCode.toUpperCase() : "VI";
        binding.tvTopDealHeading.setText("TOP RECOMMENDED DEAL FOR " + op);

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
            binding.tvDealDailyCost.setText("₹349 / 28 days = ₹8.89/day (Save ₹3.57/day)");
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
            binding.tvDealDailyCost.setText("₹349 / 28 days = ₹9.79/day (Save ₹2.68/day)");
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
            binding.tvDealDailyCost.setText("₹199 / 30 days = ₹5.80/day (Save ₹0.83/day)");
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
            binding.tvDealDailyCost.setText("₹349 / 28 days = ₹7.46/day (Save ₹5.00/day)");
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
        String mobile = currentSim.mobileNumber != null ? currentSim.mobileNumber : "9847837607";
        TelecomRedirectionHelper.redirectToRechargeCheckout(
                requireContext(),
                op,
                mobile,
                349.0,
                "Check verified " + op + " plans and app-exclusive offers"
        );
    }

    @Override
    public void onResume() {
        super.onResume();
        updateProviderHandsetStatus();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
