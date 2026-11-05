package com.bestrecharge.app.ui.compare;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.bestrecharge.app.data.local.entities.RechargePlanEntity;
import com.bestrecharge.app.data.repository.RechargeRepository;

import java.util.List;

public class CompareViewModel extends AndroidViewModel {
    private final RechargeRepository repository;

    public CompareViewModel(@NonNull Application application) {
        super(application);
        this.repository = new RechargeRepository(application);
    }

    public LiveData<List<RechargePlanEntity>> getAllPlans() {
        return repository.getAllPlans();
    }

    public LiveData<List<RechargePlanEntity>> getPlansByOperator(String operatorCode) {
        return repository.getPlansByOperator(operatorCode);
    }
}
