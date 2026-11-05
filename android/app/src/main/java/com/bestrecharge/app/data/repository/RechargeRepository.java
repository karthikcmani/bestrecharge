package com.bestrecharge.app.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.bestrecharge.app.data.local.AppDatabase;
import com.bestrecharge.app.data.local.dao.OfferDao;
import com.bestrecharge.app.data.local.dao.RechargePlanDao;
import com.bestrecharge.app.data.local.entities.OfferEntity;
import com.bestrecharge.app.data.local.entities.RechargePlanEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RechargeRepository {
    private final RechargePlanDao planDao;
    private final OfferDao offerDao;
    private final ExecutorService executor;

    public RechargeRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        this.planDao = db.rechargePlanDao();
        this.offerDao = db.offerDao();
        this.executor = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<RechargePlanEntity>> getAllPlans() {
        return planDao.getAllPlansLiveData();
    }

    public LiveData<List<RechargePlanEntity>> getPlansByOperator(String operatorCode) {
        return planDao.getPlansByOperatorLiveData(operatorCode);
    }

    public LiveData<RechargePlanEntity> getPlanById(String planId) {
        return planDao.getPlanByIdLiveData(planId);
    }

    public LiveData<List<OfferEntity>> getAllOffers() {
        return offerDao.getAllOffersLiveData();
    }

    public LiveData<List<OfferEntity>> getOffersByCategory(String category) {
        return offerDao.getOffersByCategoryLiveData(category);
    }

    public void insertOffer(OfferEntity offer) {
        executor.execute(() -> offerDao.insert(offer));
    }

    public void updateOfferTag(String offerId, String tag) {
        executor.execute(() -> offerDao.updateOfferTag(offerId, tag));
    }
}
