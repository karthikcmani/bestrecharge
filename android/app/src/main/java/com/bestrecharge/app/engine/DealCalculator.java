package com.bestrecharge.app.engine;

import com.bestrecharge.app.data.local.entities.OfferEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DealCalculator {

    public static boolean isOfferExpired(String expiryDate) {
        if (expiryDate == null || expiryDate.isEmpty()) {
            return false;
        }
        // Basic date format check YYYY-MM-DD
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
            java.util.Date exp = sdf.parse(expiryDate);
            return exp != null && exp.before(new java.util.Date());
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean evaluateUserEligibility(String userEligibility, String provider, UserPreferences prefs) {
        if (userEligibility == null || userEligibility.equalsIgnoreCase("ALL")) {
            return true;
        }
        if (prefs == null) {
            return false;
        }
        String pUpper = provider != null ? provider.toUpperCase() : "";
        if (pUpper.contains("PAYTM")) {
            if ("NEW_USER".equalsIgnoreCase(userEligibility) || "INACTIVE_60_DAYS".equalsIgnoreCase(userEligibility)) {
                return prefs.isPaytmNewOrInactive60d;
            }
        }
        if (pUpper.contains("BHIM")) {
            if ("NEW_USER".equalsIgnoreCase(userEligibility)) {
                return prefs.isBhimNewUser;
            }
        }
        if (pUpper.contains("PHONEPE")) {
            if ("NEW_USER".equalsIgnoreCase(userEligibility)) {
                return prefs.isPhonePeNewUser;
            }
        }
        return true;
    }

    public static DealBreakdown evaluateDeal(double planPrice, OfferEntity opOffer, OfferEntity payOffer,
                                            OfferEntity rewardOffer, UserPreferences prefs) {
        double originalMrp = planPrice;
        double opDiscount = 0.0;
        List<String> stacked = new ArrayList<>();

        // 1. Evaluate Operator Offer
        if (opOffer != null) {
            if (isOfferExpired(opOffer.expiryDate)) {
                return null;
            }
            if (originalMrp < opOffer.minTransactionAmount) {
                return null;
            }
            opDiscount = Math.max(0.0, opOffer.discountAmount);
            stacked.add(opOffer.title);
        }

        double immediatePayable = Math.max(0.0, originalMrp - opDiscount);

        // 2. Evaluate Payment Offer
        double payDiscount = 0.0;
        double guaranteedCashback = 0.0;
        double potentialCashback = 0.0;
        String providerName = "Standard UPI / Card";
        String eligibilityStatus = "ALL";

        if (payOffer != null) {
            if (isOfferExpired(payOffer.expiryDate)) {
                return null;
            }
            providerName = payOffer.provider != null ? payOffer.provider : "Payment Offer";

            if (!evaluateUserEligibility(payOffer.userEligibility, providerName, prefs)) {
                return null;
            }
            eligibilityStatus = payOffer.userEligibility != null ? payOffer.userEligibility : "STANDARD";

            if (originalMrp < payOffer.minTransactionAmount) {
                return null;
            }

            if ("FLAT_DISCOUNT".equalsIgnoreCase(payOffer.discountType)) {
                payDiscount = Math.max(0.0, payOffer.discountAmount);
            } else if ("FLAT_CASHBACK".equalsIgnoreCase(payOffer.discountType)) {
                guaranteedCashback = Math.max(0.0, payOffer.cashbackAmount);
            } else if ("UPTO_CASHBACK".equalsIgnoreCase(payOffer.discountType)) {
                potentialCashback = Math.max(0.0, payOffer.cashbackAmount);
            }

            stacked.add(payOffer.title);
        }

        // 3. Evaluate Reward Offer
        String rewardTitle = null;
        double rewardVal = 0.0;
        if (rewardOffer != null) {
            rewardTitle = rewardOffer.title;
            rewardVal = rewardOffer.cashbackAmount;
            stacked.add(rewardTitle);
        }

        // Financial Reconciliation
        double outOfPocket = Math.max(0.0, immediatePayable - payDiscount);
        double guaranteedEffectiveCost = Math.max(0.0, outOfPocket - guaranteedCashback);
        double potentialEffectiveCost = Math.max(0.0, guaranteedEffectiveCost - potentialCashback);

        double totalGuaranteedSavings = originalMrp - guaranteedEffectiveCost;
        double totalPotentialSavings = originalMrp - potentialEffectiveCost;

        // Formulate explanation
        List<String> expParts = new ArrayList<>();
        if (opDiscount > 0) {
            expParts.add("₹" + (int) opDiscount + " instant operator discount");
        }
        if (payDiscount > 0) {
            expParts.add("₹" + (int) payDiscount + " payment discount");
        }
        if (guaranteedCashback > 0) {
            expParts.add("₹" + (int) guaranteedCashback + " assured cashback via " + providerName);
        }
        if (potentialCashback > 0) {
            expParts.add("up to ₹" + (int) potentialCashback + " scratch-card cashback via " + providerName);
        }
        if (rewardTitle != null) {
            expParts.add("plus " + rewardTitle);
        }

        String explanation = expParts.isEmpty() ? "Standard recharge price without active promotional offers."
                : String.join(" + ", expParts);

        // Action labels directed to provider app
        String actionIntentUri = "provider://recharge";
        String actionLabel = "Recharge in Official App";
        if (opOffer != null && opOffer.provider != null) {
            actionLabel = "Recharge in " + opOffer.provider + " App";
        } else if (payOffer != null) {
            actionLabel = "Recharge via " + providerName;
        }

        return new DealBreakdown(
                originalMrp, opDiscount, immediatePayable, providerName,
                payDiscount, guaranteedCashback, potentialCashback,
                rewardTitle, rewardVal, guaranteedEffectiveCost, potentialEffectiveCost,
                totalGuaranteedSavings, totalPotentialSavings, eligibilityStatus,
                "HIGH", stacked, explanation, actionIntentUri, actionLabel
        );
    }

    public static List<DealBreakdown> findBestDeals(double planPrice, String operatorCode,
                                                    List<OfferEntity> allOffers, UserPreferences prefs) {
        List<OfferEntity> opOffers = new ArrayList<>();
        List<OfferEntity> payOffers = new ArrayList<>();
        List<OfferEntity> rewardOffers = new ArrayList<>();

        String opUpper = operatorCode != null ? operatorCode.toUpperCase() : "VI";

        for (OfferEntity o : allOffers) {
            if (!o.isActive || isOfferExpired(o.expiryDate)) continue;
            boolean matchesOp = o.applicableOperators == null || o.applicableOperators.equalsIgnoreCase("ALL")
                    || o.applicableOperators.toUpperCase().contains(opUpper);
            if (!matchesOp) continue;

            if ("OPERATOR".equalsIgnoreCase(o.category)) {
                opOffers.add(o);
            } else if ("UPI".equalsIgnoreCase(o.category) || "WALLET".equalsIgnoreCase(o.category) || "BANK_CARD".equalsIgnoreCase(o.category)) {
                payOffers.add(o);
            } else if ("REWARD".equalsIgnoreCase(o.category)) {
                rewardOffers.add(o);
            }
        }

        List<OfferEntity> opCandidates = new ArrayList<>(opOffers);
        opCandidates.add(null);

        List<OfferEntity> payCandidates = new ArrayList<>(payOffers);
        payCandidates.add(null);

        List<OfferEntity> rewardCandidates = new ArrayList<>();
        if (!rewardOffers.isEmpty()) {
            rewardCandidates.add(rewardOffers.get(0));
        }
        rewardCandidates.add(null);

        List<DealBreakdown> evaluated = new ArrayList<>();

        for (OfferEntity op : opCandidates) {
            for (OfferEntity pay : payCandidates) {
                if (op != null && pay != null && !op.isStackableWithOperator) {
                    continue;
                }
                for (OfferEntity rew : rewardCandidates) {
                    DealBreakdown deal = evaluateDeal(planPrice, op, pay, rew, prefs);
                    if (deal != null) {
                        evaluated.add(deal);
                    }
                }
            }
        }

        if (evaluated.isEmpty()) {
            DealBreakdown baseline = evaluateDeal(planPrice, null, null, null, prefs);
            if (baseline != null) evaluated.add(baseline);
        }

        // Sort: Highest guaranteed savings, then lowest guaranteed effective cost, then potential savings
        Collections.sort(evaluated, new Comparator<DealBreakdown>() {
            @Override
            public int compare(DealBreakdown d1, DealBreakdown d2) {
                int c1 = Double.compare(d2.totalGuaranteedSavings, d1.totalGuaranteedSavings);
                if (c1 != 0) return c1;
                int c2 = Double.compare(d1.guaranteedEffectiveCost, d2.guaranteedEffectiveCost);
                if (c2 != 0) return c2;
                return Double.compare(d2.totalPotentialSavings, d1.totalPotentialSavings);
            }
        });

        // Deduplicate
        List<DealBreakdown> unique = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (DealBreakdown d : evaluated) {
            String key = d.guaranteedEffectiveCost + "_" + d.potentialEffectiveCost + "_" + d.recommendedPaymentProvider;
            if (!seen.contains(key)) {
                seen.add(key);
                unique.add(d);
            }
        }

        return unique;
    }
}
