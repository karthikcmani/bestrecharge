package com.bestrecharge.app.ui.offers;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bestrecharge.app.R;
import com.bestrecharge.app.data.local.entities.OfferEntity;
import com.bestrecharge.app.databinding.FragmentOffersBinding;
import com.bestrecharge.app.ui.ocr.ScreenshotImportActivity;

import java.util.ArrayList;
import java.util.List;

public class OffersFragment extends Fragment implements OfferAdapter.OnOfferClickListener {

    private FragmentOffersBinding binding;
    private OffersViewModel viewModel;
    private OfferAdapter adapter;
    private List<OfferEntity> allOffers = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentOffersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(OffersViewModel.class);

        adapter = new OfferAdapter(this);
        binding.rvOffers.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvOffers.setAdapter(adapter);

        viewModel.getAllOffers().observe(getViewLifecycleOwner(), offers -> {
            if (offers != null) {
                allOffers = offers;
                adapter.setOffers(offers);
            }
        });

        // Filter chips
        binding.chipGroupOfferCategories.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chip_cat_upi)) {
                filterCategory("UPI");
            } else if (checkedIds.contains(R.id.chip_cat_operator)) {
                filterCategory("OPERATOR");
            } else if (checkedIds.contains(R.id.chip_cat_reward)) {
                filterCategory("REWARD");
            } else {
                adapter.setOffers(allOffers);
            }
        });

        binding.fabImportScreenshot.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), ScreenshotImportActivity.class);
            startActivity(intent);
        });
    }

    private void filterCategory(String category) {
        List<OfferEntity> filtered = new ArrayList<>();
        for (OfferEntity o : allOffers) {
            if (o.category != null && o.category.equalsIgnoreCase(category)) {
                filtered.add(o);
            }
        }
        adapter.setOffers(filtered);
    }

    @Override
    public void onViewTerms(OfferEntity offer) {
        OfferDetailBottomSheet sheet = new OfferDetailBottomSheet(offer);
        sheet.show(getParentFragmentManager(), "OfferDetailSheet");
    }

    @Override
    public void onMarkUsed(OfferEntity offer) {
        viewModel.updateOfferTag(offer.id, "USED");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
