package com.bestrecharge.app.ui.savings;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.bestrecharge.app.data.local.entities.SavingsRecordEntity;
import com.bestrecharge.app.data.repository.SavingsRepository;

import java.util.List;

public class SavingsViewModel extends AndroidViewModel {
    private final SavingsRepository repository;

    public SavingsViewModel(@NonNull Application application) {
        super(application);
        this.repository = new SavingsRepository(application);
    }

    public LiveData<List<SavingsRecordEntity>> getAllRecords() {
        return repository.getAllRecords();
    }

    public void updateStatus(String recordId, String status, double receivedAmount, double effectiveCost) {
        repository.updateRecordStatus(recordId, status, receivedAmount, effectiveCost);
    }
}
