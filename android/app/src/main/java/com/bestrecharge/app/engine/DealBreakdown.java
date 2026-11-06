package com.bestrecharge.app.engine;

import com.bestrecharge.app.data.local.entities.RechargePlanEntity;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;

public class DealBreakdown implements Serializable {
    public RechargePlanEntity rechargePlan;
    public double originalMrp;
    public double operatorDiscount;
    public double immediatePayable;
    public String recommendedPaymentProvider;
    public double appliedPaymentDiscount;
    public double guaranteedCashback;
    public double potentialCashback;
    public String rewardPointsTitle;
    public double rewardPointsValue;
    public double guaranteedEffectiveCost;
    public double potentialEffectiveCost;
    public double totalGuaranteedSavings;
    public double totalPotentialSavings;
    public String userEligibilityStatus;
    public String confidenceLevel;
    public List<String> stackedOffers;
    public String explanation;
    public String actionIntentUri;
    public String actionLabel;
    public String whyRecommended;
    public int validityDays = 28;

    public double originalCostPerDay;
    public double payableCostPerDay;
    public double effectiveCostPerDay;

    public DealBreakdown(double originalMrp, double operatorDiscount, double immediatePayable,
                         String recommendedPaymentProvider, double appliedPaymentDiscount,
                         double guaranteedCashback, double potentialCashback,
                         String rewardPointsTitle, double rewardPointsValue,
                         double guaranteedEffectiveCost, double potentialEffectiveCost,
                         double totalGuaranteedSavings, double totalPotentialSavings,
                         String userEligibilityStatus, String confidenceLevel,
                         List<String> stackedOffers, String explanation,
                         String actionIntentUri, String actionLabel) {
        this.originalMrp = originalMrp;
        this.operatorDiscount = operatorDiscount;
        this.immediatePayable = immediatePayable;
        this.recommendedPaymentProvider = recommendedPaymentProvider;
        this.appliedPaymentDiscount = appliedPaymentDiscount;
        this.guaranteedCashback = guaranteedCashback;
        this.potentialCashback = potentialCashback;
        this.rewardPointsTitle = rewardPointsTitle;
        this.rewardPointsValue = rewardPointsValue;
        this.guaranteedEffectiveCost = guaranteedEffectiveCost;
        this.potentialEffectiveCost = potentialEffectiveCost;
        this.totalGuaranteedSavings = totalGuaranteedSavings;
        this.totalPotentialSavings = totalPotentialSavings;
        this.userEligibilityStatus = userEligibilityStatus;
        this.confidenceLevel = confidenceLevel;
        this.stackedOffers = stackedOffers;
        this.explanation = explanation;
        this.actionIntentUri = actionIntentUri;
        this.actionLabel = actionLabel;
        this.validityDays = 28;
        recalculateDailyCosts();
    }

    public void recalculateDailyCosts() {
        int v = validityDays > 0 ? validityDays : 1;
        this.originalCostPerDay = Math.round((originalMrp / v) * 100.0) / 100.0;
        this.payableCostPerDay = Math.round((immediatePayable / v) * 100.0) / 100.0;
        this.effectiveCostPerDay = Math.round((potentialEffectiveCost / v) * 100.0) / 100.0;
    }

    public double getDailyMrpCost() {
        return originalCostPerDay;
    }

    public double getDailyPayableCost() {
        return payableCostPerDay;
    }

    public double getDailyEffectiveCost() {
        return effectiveCostPerDay;
    }

    public double getDailySavings() {
        return Math.round((originalCostPerDay - effectiveCostPerDay) * 100.0) / 100.0;
    }

    /**
     * Clear, transparent multi-tier daily financial metric as required by Section 11:
     * Never equating 349 / 28 = 7.46, but explicitly separating MRP rate, Payable rate, and Effective rate.
     */
    public String getDailyCostComparisonString() {
        int v = validityDays > 0 ? validityDays : 1;
        return String.format(Locale.US,
                "Original: ₹%.2f/d • Payable: ₹%.2f/d • Effective: ₹%.2f/d (Save ₹%.2f/d)",
                originalCostPerDay, payableCostPerDay, effectiveCostPerDay, getDailySavings());
    }
}
