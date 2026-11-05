package com.bestrecharge.app.ui.compare;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bestrecharge.app.data.local.entities.RechargePlanEntity;
import com.bestrecharge.app.databinding.ItemPlanCardBinding;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PlanAdapter extends RecyclerView.Adapter<PlanAdapter.PlanViewHolder> {

    public interface OnPlanActionListener {
        void onFindBestDeal(RechargePlanEntity plan);
        void onCompareSelectionChanged(List<RechargePlanEntity> selectedPlans);
    }

    private List<RechargePlanEntity> plans = new ArrayList<>();
    private final Set<String> selectedPlanIds = new HashSet<>();
    private final OnPlanActionListener listener;

    public PlanAdapter(OnPlanActionListener listener) {
        this.listener = listener;
    }

    public void setPlans(List<RechargePlanEntity> newPlans) {
        this.plans = newPlans != null ? newPlans : new ArrayList<>();
        notifyDataSetChanged();
    }

    public List<RechargePlanEntity> getSelectedPlans() {
        List<RechargePlanEntity> selected = new ArrayList<>();
        for (RechargePlanEntity p : plans) {
            if (selectedPlanIds.contains(p.id)) {
                selected.add(p);
            }
        }
        return selected;
    }

    @NonNull
    @Override
    public PlanViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPlanCardBinding binding = ItemPlanCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new PlanViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull PlanViewHolder holder, int position) {
        holder.bind(plans.get(position));
    }

    @Override
    public int getItemCount() {
        return plans.size();
    }

    class PlanViewHolder extends RecyclerView.ViewHolder {
        private final ItemPlanCardBinding binding;

        public PlanViewHolder(ItemPlanCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(RechargePlanEntity plan) {
            binding.tvOperatorBadge.setText(plan.operatorCode != null ? plan.operatorCode.toUpperCase() : "VI");
            binding.tvPlanCategory.setText(plan.planCategory != null ? plan.planCategory.replace("_", " ") : "POPULAR");
            binding.tvPrice.setText("₹" + (int) plan.price);
            binding.tvValidity.setText("• " + plan.validityDays + " Days Validity");
            binding.tvDailyData.setText(plan.dailyDataGb > 0 ? plan.dailyDataGb + " GB/day" : plan.totalDataGb + " GB Total");
            binding.tvVoiceSms.setText("• " + plan.voiceBenefit + " • " + plan.smsPerDay + " SMS/day");

            if (plan.ottBenefits != null && !plan.ottBenefits.isEmpty()) {
                binding.tvOttBenefits.setVisibility(View.VISIBLE);
                binding.tvOttBenefits.setText(plan.ottBenefits);
            } else {
                binding.tvOttBenefits.setVisibility(View.GONE);
            }

            binding.tvSourceVerified.setText("Source: " + (plan.source != null ? plan.source : "Verified Portal"));

            binding.cbCompareSelect.setOnCheckedChangeListener(null);
            binding.cbCompareSelect.setChecked(selectedPlanIds.contains(plan.id));

            binding.cbCompareSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    if (selectedPlanIds.size() >= 3) {
                        buttonView.setChecked(false);
                        return;
                    }
                    selectedPlanIds.add(plan.id);
                } else {
                    selectedPlanIds.remove(plan.id);
                }
                if (listener != null) {
                    listener.onCompareSelectionChanged(getSelectedPlans());
                }
            });

            binding.btnFindBestDeal.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onFindBestDeal(plan);
                }
            });
        }
    }
}
