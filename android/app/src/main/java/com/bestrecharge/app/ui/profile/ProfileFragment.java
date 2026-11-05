package com.bestrecharge.app.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bestrecharge.app.data.local.entities.SimProfileEntity;
import com.bestrecharge.app.databinding.FragmentProfileBinding;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private ProfileViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        viewModel.getAllSims().observe(getViewLifecycleOwner(), sims -> {
            if (sims != null) {
                if (sims.size() > 0) {
                    SimProfileEntity s1 = sims.get(0);
                    binding.tvProfSim1Number.setText("+91 " + s1.mobileNumber + " (" + s1.simSlot + (s1.isPrimary ? " - Primary)" : ")"));
                    binding.tvProfSim1Op.setText(s1.operatorName + " • " + s1.circleName);
                }
                if (sims.size() > 1) {
                    SimProfileEntity s2 = sims.get(1);
                    binding.tvProfSim2Number.setText("+91 " + s2.mobileNumber + " (" + s2.simSlot + (s2.isPrimary ? " - Primary)" : ")"));
                    binding.tvProfSim2Op.setText(s2.operatorName + " • " + s2.circleName);
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
