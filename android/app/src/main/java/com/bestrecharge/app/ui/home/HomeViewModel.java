package com.bestrecharge.app.ui.home;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.bestrecharge.app.data.local.entities.OfferEntity;
import com.bestrecharge.app.data.local.entities.RechargePlanEntity;
import com.bestrecharge.app.data.local.entities.SimProfileEntity;
import com.bestrecharge.app.data.repository.RechargeRepository;
import com.bestrecharge.app.data.repository.SimRepository;

import java.util.List;

public class HomeViewModel extends AndroidViewModel {
    private final SimRepository simRepository;
    private final RechargeRepository rechargeRepository;

    public HomeViewModel(@NonNull Application application) {
        super(application);
        this.simRepository = new SimRepository(application);
        this.rechargeRepository = new RechargeRepository(application);
    }

    public LiveData<SimProfileEntity> getPrimarySim() {
        return simRepository.getPrimarySimProfile();
    }

    public LiveData<List<SimProfileEntity>> getAllSims() {
        return simRepository.getAllSimProfiles();
    }

    public void setPrimarySim(String simId) {
        simRepository.setPrimarySim(simId);
    }

    public LiveData<List<RechargePlanEntity>> getAllPlans() {
        return rechargeRepository.getAllPlans();
    }

    public LiveData<List<OfferEntity>> getAllOffers() {
        return rechargeRepository.getAllOffers();
    }
}
