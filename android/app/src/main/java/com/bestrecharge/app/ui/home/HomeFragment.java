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
import com.bestrecharge.app.data.local.entities.RechargePlanEntity;
import com.bestrecharge.app.data.local.entities.SimProfileEntity;
import com.bestrecharge.app.databinding.FragmentHomeBinding;
import com.bestrecharge.app.engine.DealBreakdown;
import com.bestrecharge.app.engine.RechargePreference;
import com.bestrecharge.app.engine.RecommendationEngine;
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

        viewModel.getRecommendationResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null && result.topDeal != null) {
                bindTopDeal(result.topDeal);
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
        viewModel.setOperator(sim.operatorCode);
    }

    private void updateProviderHandsetStatus() {
        if (currentSim == null || getContext() == null) return;
        String op = currentSim.operatorCode != null ? currentSim.operatorCode.toUpperCase() : "VI";
        String opName = TelecomRedirectionHelper.getOperatorDisplayName(op);
        boolean installed = TelecomRedirectionHelper.isProviderAppInstalled(getContext(), op);

        if (installed) {
            binding.tvProviderHandsetStatus.setText("● " + opName + " detected on handset • Verified tariff active");
            binding.tvProviderHandsetStatus.setTextColor(getResources().getColor(R.color.savings_green_dark, null));
            binding.btnProviderHandsetAction.setText("Open " + opName);
        } else {
            binding.tvProviderHandsetStatus.setText("⚠️ " + opName + " not installed (Sourced data active)");
            binding.tvProviderHandsetStatus.setTextColor(getResources().getColor(R.color.on_surface_muted, null));
            binding.btnProviderHandsetAction.setText("Install App");
        }
    }

    private void handleProviderHandsetAction() {
        if (currentSim == null || getContext() == null) return;
        String op = currentSim.operatorCode != null ? currentSim.operatorCode.toUpperCase() : "VI";
        boolean installed = TelecomRedirectionHelper.isProviderAppInstalled(getContext(), op);

        if (installed) {
            openOfficialTelecomApp();
        } else {
            String pkg = TelecomRedirectionHelper.getPackageForOperator(op);
            TelecomRedirectionHelper.openPlayStore(getContext(), pkg);
        }
    }

    private void handlePreferenceChanged(int chipId) {
        if (currentSim == null) return;
        String op = currentSim.operatorCode != null ? currentSim.operatorCode : "VI";
        RechargePreference pref;

        if (chipId == R.id.chip_pref_validity) {
            pref = new RechargePreference(
                    RechargePreference.DataAllowance.VALIDITY_ONLY, 0, 0.0,
                    RechargePreference.RankingPriority.MAXIMUM_VALIDITY, op
            );
            triggerProcessingAnimation("Filtering for maximum validity plans & calculating daily rates...");
        } else if (chipId == R.id.chip_pref_under_300) {
            pref = new RechargePreference(
                    RechargePreference.DataAllowance.ANY, 0, 300.0,
                    RechargePreference.RankingPriority.LOWEST_EFFECTIVE_COST, op
            );
            triggerProcessingAnimation("Filtering plans under ₹300 & calculating maximum savings...");
        } else if (chipId == R.id.chip_pref_1_5_gb) {
            pref = new RechargePreference(
                    RechargePreference.DataAllowance.GB_1_5_DAY, 0, 0.0,
                    RechargePreference.RankingPriority.BEST_OVERALL_VALUE, op
            );
            triggerProcessingAnimation("Filtering 1.5 GB/day plans & stacking verified offers...");
        } else if (chipId == R.id.chip_pref_1_gb) {
            pref = new RechargePreference(
                    RechargePreference.DataAllowance.GB_1_DAY, 0, 0.0,
                    RechargePreference.RankingPriority.BEST_OVERALL_VALUE, op
            );
            triggerProcessingAnimation("Filtering 1 GB/day plans & calculating effective cost...");
        } else if (chipId == R.id.chip_pref_long_term) {
            pref = new RechargePreference(
                    RechargePreference.DataAllowance.ANY, 84, 0.0,
                    RechargePreference.RankingPriority.MAXIMUM_VALIDITY, op
            );
            triggerProcessingAnimation("Filtering 84+ days long term plans & analyzing value...");
        } else {
            pref = new RechargePreference(
                    RechargePreference.DataAllowance.ANY, 0, 0.0,
                    RechargePreference.RankingPriority.BEST_OVERALL_VALUE, op
            );
            triggerProcessingAnimation("Evaluating all plans across data, validity & cashback...");
        }

        viewModel.setRechargePreference(pref);
    }

    private void triggerProcessingAnimation(String statusText) {
        binding.cardProcessingState.setVisibility(View.VISIBLE);
        binding.tvProcessingStatus.setText(statusText);

        binding.cardProcessingState.postDelayed(() -> {
            if (binding != null) {
                binding.cardProcessingState.setVisibility(View.GONE);
            }
        }, 600);
    }

    private void bindTopDeal(DealBreakdown deal) {
        if (deal == null || deal.rechargePlan == null) return;
        binding.cardProcessingState.setVisibility(View.GONE);

        RechargePlanEntity plan = deal.rechargePlan;
        currentPlanId = plan.id;

        binding.tvTopDealHeading.setText("TOP RECOMMENDED DEAL FOR " + plan.operatorName.toUpperCase());
        binding.tvDealBadgeLabel.setText(deal.actionLabel != null ? deal.actionLabel : "BEST DEAL");
        binding.tvDealSaveAmount.setText("Save ₹" + (int) (deal.originalMrp - deal.potentialEffectiveCost));
        binding.tvDealPlanTitle.setText(plan.operatorName + " " + (plan.planCategory != null ? plan.planCategory.replace("_", " ") : "") + " ₹" + (int) plan.price);

        String benefits = plan.validityDays + " Days • " +
                (plan.dailyDataGb > 0 ? plan.dailyDataGb + "GB/day" : plan.totalDataGb + "GB Total") +
                " • " + plan.voiceBenefit;
        if (plan.ottBenefits != null && !plan.ottBenefits.isEmpty()) {
            benefits += " • " + plan.ottBenefits;
        }
        binding.tvDealBenefits.setText(benefits);

        binding.tvMrpPrice.setText("₹" + (int) deal.originalMrp);
        binding.tvPayablePrice.setText("₹" + (int) deal.immediatePayable);
        binding.tvEffectivePrice.setText("₹" + (int) deal.potentialEffectiveCost);

        binding.tvDealDailyCost.setText(deal.getDailyCostComparisonString());
        binding.tvDealStackExplanation.setText(deal.explanation + (deal.whyRecommended != null ? " • " + deal.whyRecommended : ""));
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
