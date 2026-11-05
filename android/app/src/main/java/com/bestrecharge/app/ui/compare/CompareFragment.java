package com.bestrecharge.app.ui.compare;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bestrecharge.app.R;
import com.bestrecharge.app.data.local.entities.RechargePlanEntity;
import com.bestrecharge.app.data.local.entities.SimProfileEntity;
import com.bestrecharge.app.databinding.DialogComparePlansBinding;
import com.bestrecharge.app.databinding.FragmentCompareBinding;
import com.bestrecharge.app.ui.deals.BestDealActivity;

import java.util.ArrayList;
import java.util.List;

public class CompareFragment extends Fragment implements PlanAdapter.OnPlanActionListener {

    private FragmentCompareBinding binding;
    private CompareViewModel viewModel;
    private PlanAdapter adapter;
    private List<RechargePlanEntity> currentPlans = new ArrayList<>();
    private List<RechargePlanEntity> selectedForCompare = new ArrayList<>();
    private SimProfileEntity activeSim;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCompareBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(CompareViewModel.class);

        adapter = new PlanAdapter(this);
        binding.rvPlans.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvPlans.setAdapter(adapter);

        viewModel.getAllPlans().observe(getViewLifecycleOwner(), plans -> {
            if (plans != null) {
                currentPlans = plans;
                adapter.setPlans(plans);
            }
        });

        // Setup filter chips
        binding.chipGroupOperators.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chip_op_vi)) {
                filterByOperator("VI");
            } else if (checkedIds.contains(R.id.chip_op_jio)) {
                filterByOperator("JIO");
            } else if (checkedIds.contains(R.id.chip_op_airtel)) {
                filterByOperator("AIRTEL");
            } else if (checkedIds.contains(R.id.chip_op_bsnl)) {
                filterByOperator("BSNL");
            } else {
                adapter.setPlans(currentPlans);
            }
        });

        binding.btnTriggerCompare.setOnClickListener(v -> showComparisonDialog());

        new com.bestrecharge.app.data.repository.SimRepository(requireActivity().getApplication())
                .getPrimarySimProfile()
                .observe(getViewLifecycleOwner(), sim -> {
                    if (sim != null) {
                        activeSim = sim;
                        binding.tvCompareActiveSim.setText("Checking for: " + sim.operatorName + " (+91 " + (sim.mobileNumber != null ? sim.mobileNumber : "") + ")");
                    }
                });

        binding.btnCompareChangeNumber.setOnClickListener(v -> {
            if (getContext() != null) {
                new com.bestrecharge.app.ui.dialogs.ChangeNumberDialog(getContext(), activeSim, selectedSim -> {
                    activeSim = selectedSim;
                    binding.tvCompareActiveSim.setText("Checking for: " + selectedSim.operatorName + " (+91 " + selectedSim.mobileNumber + ")");
                    // Auto-filter by new operator
                    filterByOperator(selectedSim.operatorCode);
                }).show();
            }
        });
    }

    private void filterByOperator(String operatorCode) {
        List<RechargePlanEntity> filtered = new ArrayList<>();
        for (RechargePlanEntity p : currentPlans) {
            if (p.operatorCode != null && p.operatorCode.equalsIgnoreCase(operatorCode)) {
                filtered.add(p);
            }
        }
        adapter.setPlans(filtered);
    }

    @Override
    public void onFindBestDeal(RechargePlanEntity plan) {
        Intent intent = new Intent(getContext(), BestDealActivity.class);
        intent.putExtra("PLAN_ID", plan.id);
        startActivity(intent);
    }

    @Override
    public void onCompareSelectionChanged(List<RechargePlanEntity> selectedPlans) {
        this.selectedForCompare = selectedPlans;
        if (selectedPlans.size() >= 2) {
            binding.cardCompareBar.setVisibility(View.VISIBLE);
            binding.tvCompareCount.setText(selectedPlans.size() + " plans selected for comparison");
        } else {
            binding.cardCompareBar.setVisibility(View.GONE);
        }
    }

    private void showComparisonDialog() {
        if (selectedForCompare.size() < 2 || getContext() == null) return;

        DialogComparePlansBinding dialogBinding = DialogComparePlansBinding.inflate(getLayoutInflater());
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogBinding.getRoot())
                .create();

        LinearLayout columnsLayout = dialogBinding.llCompareColumns;
        columnsLayout.removeAllViews();

        for (RechargePlanEntity p : selectedForCompare) {
            View colView = getLayoutInflater().inflate(R.layout.item_plan_card, columnsLayout, false);
            // Customize column view width
            colView.setLayoutParams(new LinearLayout.LayoutParams(600, ViewGroup.LayoutParams.WRAP_CONTENT));
            colView.setPadding(8, 8, 8, 8);

            TextView tvPrice = colView.findViewById(R.id.tv_price);
            TextView tvValidity = colView.findViewById(R.id.tv_validity);
            TextView tvData = colView.findViewById(R.id.tv_daily_data);
            TextView tvVoice = colView.findViewById(R.id.tv_voice_sms);
            TextView tvOp = colView.findViewById(R.id.tv_operator_badge);

            tvPrice.setText("₹" + (int) p.price + "\n(₹" + String.format("%.1f", p.price / p.validityDays) + "/day)");
            tvValidity.setText(p.validityDays + " Days");
            tvData.setText(p.dailyDataGb > 0 ? p.dailyDataGb + " GB/day" : p.totalDataGb + " GB Total");
            tvVoice.setText(p.voiceBenefit);
            tvOp.setText(p.operatorCode);

            colView.findViewById(R.id.cb_compare_select).setVisibility(View.GONE);
            colView.findViewById(R.id.btn_find_best_deal).setOnClickListener(v -> {
                dialog.dismiss();
                onFindBestDeal(p);
            });

            columnsLayout.addView(colView);
        }

        dialogBinding.btnCloseCompare.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
