package com.bestrecharge.app.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.bestrecharge.app.data.local.AppDatabase;
import com.bestrecharge.app.data.local.dao.SavingsRecordDao;
import com.bestrecharge.app.data.local.entities.SavingsRecordEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SavingsRepository {
    private final SavingsRecordDao savingsDao;
    private final ExecutorService executor;

    public SavingsRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        this.savingsDao = db.savingsRecordDao();
        this.executor = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<SavingsRecordEntity>> getAllRecords() {
        return savingsDao.getAllRecordsLiveData();
    }

    public void insertRecord(SavingsRecordEntity record) {
        executor.execute(() -> savingsDao.insert(record));
    }

    public void updateRecordStatus(String recordId, String status, double receivedAmount, double effectiveCost) {
        executor.execute(() -> savingsDao.updateRecordStatus(recordId, status, receivedAmount, effectiveCost));
    }
}
