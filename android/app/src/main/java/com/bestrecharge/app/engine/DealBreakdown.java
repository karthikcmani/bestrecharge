package com.bestrecharge.app.engine;

import java.io.Serializable;
import java.util.List;

public class DealBreakdown implements Serializable {
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
    public int validityDays = 28;

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
    }

    public double getDailyMrpCost() {
        int v = validityDays > 0 ? validityDays : 28;
        return Math.round((originalMrp / v) * 100.0) / 100.0;
    }

    public double getDailyEffectiveCost() {
        int v = validityDays > 0 ? validityDays : 28;
        return Math.round((potentialEffectiveCost / v) * 100.0) / 100.0;
    }

    public double getDailySavings() {
        int v = validityDays > 0 ? validityDays : 28;
        return Math.round(((originalMrp - potentialEffectiveCost) / v) * 100.0) / 100.0;
    }

    public String getDailyCostComparisonString() {
        int v = validityDays > 0 ? validityDays : 28;
        return "₹" + (int) originalMrp + " / " + v + " days = ₹" + String.format(java.util.Locale.US, "%.2f", getDailyEffectiveCost()) + "/day (Save ₹" + String.format(java.util.Locale.US, "%.2f", getDailySavings()) + "/day)";
    }
}
