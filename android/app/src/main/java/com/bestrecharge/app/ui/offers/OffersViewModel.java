package com.bestrecharge.app.ui.offers;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.bestrecharge.app.data.local.entities.OfferEntity;
import com.bestrecharge.app.data.repository.RechargeRepository;

import java.util.List;

public class OffersViewModel extends AndroidViewModel {
    private final RechargeRepository repository;

    public OffersViewModel(@NonNull Application application) {
        super(application);
        this.repository = new RechargeRepository(application);
    }

    public LiveData<List<OfferEntity>> getAllOffers() {
        return repository.getAllOffers();
    }

    public LiveData<List<OfferEntity>> getOffersByCategory(String category) {
        return repository.getOffersByCategory(category);
    }

    public void updateOfferTag(String offerId, String tag) {
        repository.updateOfferTag(offerId, tag);
    }
}
