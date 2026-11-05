package com.bestrecharge.app.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bestrecharge.app.R;
import com.bestrecharge.app.data.local.entities.SimProfileEntity;
import com.bestrecharge.app.databinding.FragmentHomeBinding;
import com.bestrecharge.app.ui.deals.BestDealActivity;
import com.bestrecharge.app.ui.main.MainActivity;
import com.bestrecharge.app.ui.ocr.ScreenshotImportActivity;

import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;
    private List<SimProfileEntity> allSims;
    private SimProfileEntity currentSim;

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
                binding.tvSimSlot.setText(sim.simSlot != null ? sim.simSlot.toUpperCase() : "PRIMARY SIM");
                binding.tvMobileNumber.setText("+91 " + sim.mobileNumber);
                binding.tvOperatorCircle.setText(sim.operatorName + " • " + sim.circleName + (sim.isPrepaid ? " (Prepaid)" : " (Postpaid)"));
            }
        });

        binding.btnSwitchSim.setOnClickListener(v -> {
            if (allSims != null && allSims.size() > 1 && currentSim != null) {
                for (SimProfileEntity s : allSims) {
                    if (!s.id.equals(currentSim.id)) {
                        viewModel.setPrimarySim(s.id);
                        break;
                    }
                }
            }
        });

        // Click top deal to inspect & continue
        binding.btnInspectDeal.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), BestDealActivity.class);
            intent.putExtra("PLAN_ID", "vi_349_hero");
            startActivity(intent);
        });

        binding.cardTopDeal.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), BestDealActivity.class);
            intent.putExtra("PLAN_ID", "vi_349_hero");
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
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
