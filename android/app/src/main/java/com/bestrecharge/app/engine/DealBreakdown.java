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
    }
}
