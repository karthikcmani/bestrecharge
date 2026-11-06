package com.bestrecharge.app.ui.home;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.bestrecharge.app.data.local.entities.OfferEntity;
import com.bestrecharge.app.data.local.entities.RechargePlanEntity;
import com.bestrecharge.app.data.local.entities.SimProfileEntity;
import com.bestrecharge.app.data.repository.RechargeRepository;
import com.bestrecharge.app.data.repository.SimRepository;
import com.bestrecharge.app.engine.RechargePreference;
import com.bestrecharge.app.engine.RecommendationEngine;
import com.bestrecharge.app.engine.UserPreferences;

import java.util.List;

public class HomeViewModel extends AndroidViewModel {

    private final SimRepository simRepository;
    private final RechargeRepository rechargeRepository;

    private final MutableLiveData<RechargePreference> rechargePreferenceLiveData = new MutableLiveData<>();
    private final MediatorLiveData<RecommendationEngine.RecommendationResult> recommendationResultLiveData = new MediatorLiveData<>();

    private List<RechargePlanEntity> cachedPlans;
    private List<OfferEntity> cachedOffers;

    public HomeViewModel(@NonNull Application application) {
        super(application);
        this.simRepository = new SimRepository(application);
        this.rechargeRepository = new RechargeRepository(application);

        // Initial default preference
        rechargePreferenceLiveData.setValue(new RechargePreference(
                RechargePreference.DataAllowance.ANY, 0, 0,
                RechargePreference.RankingPriority.BEST_OVERALL_VALUE, "VI"
        ));

        // Setup reactive mediator pipeline
        setupRecommendationPipeline();
    }

    private void setupRecommendationPipeline() {
        LiveData<List<RechargePlanEntity>> plansSource = rechargeRepository.getAllPlans();
        LiveData<List<OfferEntity>> offersSource = rechargeRepository.getAllOffers();

        recommendationResultLiveData.addSource(plansSource, plans -> {
            this.cachedPlans = plans;
            recalculateRecommendations();
        });

        recommendationResultLiveData.addSource(offersSource, offers -> {
            this.cachedOffers = offers;
            recalculateRecommendations();
        });

        recommendationResultLiveData.addSource(rechargePreferenceLiveData, pref -> {
            recalculateRecommendations();
        });
    }

    public void recalculateRecommendations() {
        if (cachedPlans == null || cachedOffers == null) return;
        RechargePreference currentPref = rechargePreferenceLiveData.getValue();
        if (currentPref == null) {
            currentPref = new RechargePreference();
        }

        UserPreferences userPrefs = new UserPreferences();
        RecommendationEngine.RecommendationResult result = RecommendationEngine.evaluateAndRank(
                cachedPlans, cachedOffers, userPrefs, currentPref
        );
        recommendationResultLiveData.setValue(result);
    }

    public LiveData<RecommendationEngine.RecommendationResult> getRecommendationResult() {
        return recommendationResultLiveData;
    }

    public LiveData<RechargePreference> getRechargePreference() {
        return rechargePreferenceLiveData;
    }

    public void setRechargePreference(RechargePreference preference) {
        if (preference != null) {
            rechargePreferenceLiveData.setValue(preference);
        }
    }

    public void setOperator(String operatorCode) {
        RechargePreference cur = rechargePreferenceLiveData.getValue();
        if (cur == null) cur = new RechargePreference();
        cur.operatorCode = operatorCode != null ? operatorCode.toUpperCase() : "VI";
        rechargePreferenceLiveData.setValue(cur);
    }

    public void setDataAllowancePreference(RechargePreference.DataAllowance allowance) {
        RechargePreference cur = rechargePreferenceLiveData.getValue();
        if (cur == null) cur = new RechargePreference();
        cur.dataAllowance = allowance;
        if (allowance == RechargePreference.DataAllowance.VALIDITY_ONLY) {
            cur.priority = RechargePreference.RankingPriority.MAXIMUM_VALIDITY;
        }
        rechargePreferenceLiveData.setValue(cur);
    }

    public void setBudgetLimit(double maxBudget) {
        RechargePreference cur = rechargePreferenceLiveData.getValue();
        if (cur == null) cur = new RechargePreference();
        cur.maxBudget = maxBudget;
        cur.priority = RechargePreference.RankingPriority.LOWEST_EFFECTIVE_COST;
        rechargePreferenceLiveData.setValue(cur);
    }

    public void setRankingPriority(RechargePreference.RankingPriority priority) {
        RechargePreference cur = rechargePreferenceLiveData.getValue();
        if (cur == null) cur = new RechargePreference();
        cur.priority = priority;
        rechargePreferenceLiveData.setValue(cur);
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
