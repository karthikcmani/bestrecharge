package com.bestrecharge.app.ui.offers;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bestrecharge.app.data.local.entities.OfferEntity;
import com.bestrecharge.app.databinding.BottomSheetOfferDetailBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class OfferDetailBottomSheet extends BottomSheetDialogFragment {

    private BottomSheetOfferDetailBinding binding;
    private final OfferEntity offer;

    public OfferDetailBottomSheet(OfferEntity offer) {
        this.offer = offer;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomSheetOfferDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (offer != null) {
            binding.tvSheetTitle.setText(offer.title);
            binding.tvSheetProvider.setText("Provided by " + offer.provider + " • Category: " + offer.category);
            binding.tvSheetTerms.setText(offer.termsAndConditions != null && !offer.termsAndConditions.isEmpty()
                    ? offer.termsAndConditions : "No additional terms specified.");
        }
        binding.btnSheetDismiss.setOnClickListener(v -> dismiss());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
