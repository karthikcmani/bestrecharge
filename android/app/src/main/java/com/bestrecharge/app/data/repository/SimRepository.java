package com.bestrecharge.app.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.bestrecharge.app.data.local.AppDatabase;
import com.bestrecharge.app.data.local.dao.SimProfileDao;
import com.bestrecharge.app.data.local.entities.SimProfileEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SimRepository {
    private final SimProfileDao simDao;
    private final ExecutorService executor;

    public SimRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        this.simDao = db.simProfileDao();
        this.executor = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<SimProfileEntity>> getAllSimProfiles() {
        return simDao.getAllSimProfilesLiveData();
    }

    public LiveData<SimProfileEntity> getPrimarySimProfile() {
        return simDao.getPrimarySimProfileLiveData();
    }

    public void insertOrUpdateSim(SimProfileEntity sim) {
        executor.execute(() -> {
            if (sim.isPrimary) {
                simDao.resetAllPrimary();
            }
            simDao.insertOrUpdate(sim);
        });
    }

    public void setPrimarySim(String simId) {
        executor.execute(() -> {
            simDao.resetAllPrimary();
            simDao.setPrimary(simId);
        });
    }

    public void deleteSim(SimProfileEntity sim) {
        executor.execute(() -> simDao.delete(sim));
    }
}
