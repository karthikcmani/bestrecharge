package com.bestrecharge.app.engine;

import com.bestrecharge.app.data.local.entities.OfferEntity;
import com.bestrecharge.app.data.local.entities.RechargePlanEntity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * RecommendationEngine dynamically evaluates, filters, and ranks recharge plans
 * against active operator and payment offers based on user preferences.
 */
public class RecommendationEngine {

    public static class RecommendationResult implements Serializable {
        public DealBreakdown topDeal;
        public DealBreakdown cheapestGuaranteedDeal;
        public DealBreakdown maximumValidityDeal;
        public DealBreakdown bestDataValueDeal;
        public DealBreakdown maximumCashbackDeal;
        public List<DealBreakdown> allRankedDeals = new ArrayList<>();
        public int totalPlansEvaluated;
        public int matchingPlansCount;
    }

    public static RecommendationResult evaluateAndRank(
            List<RechargePlanEntity> allPlans,
            List<OfferEntity> allOffers,
            UserPreferences userPrefs,
            RechargePreference rechargePref
    ) {
        RecommendationResult result = new RecommendationResult();
        if (allPlans == null || allPlans.isEmpty()) {
            return result;
        }

        result.totalPlansEvaluated = allPlans.size();

        // 1. Filter plans according to user criteria
        List<RechargePlanEntity> eligiblePlans = PlanFilterEngine.filterPlans(allPlans, rechargePref);
        result.matchingPlansCount = eligiblePlans.size();

        if (eligiblePlans.isEmpty()) {
            // If strict filtering returned no plans (e.g. extremely low budget),
            // fallback to lowest price plan of operator with clear note
            eligiblePlans = PlanFilterEngine.filterPlans(allPlans, new RechargePreference(
                    RechargePreference.DataAllowance.ANY, 0, 0,
                    RechargePreference.RankingPriority.LOWEST_EFFECTIVE_COST,
                    rechargePref != null ? rechargePref.operatorCode : "VI"
            ));
        }

        // 2. Evaluate deals for each eligible plan
        List<DealBreakdown> evaluatedDeals = new ArrayList<>();
        UserPreferences prefs = userPrefs != null ? userPrefs : new UserPreferences();

        for (RechargePlanEntity plan : eligiblePlans) {
            List<DealBreakdown> dealsForPlan = DealCalculator.findBestDeals(
                    plan.price, plan.operatorCode, allOffers, prefs
            );

            DealBreakdown bestDealForPlan;
            if (dealsForPlan != null && !dealsForPlan.isEmpty()) {
                bestDealForPlan = dealsForPlan.get(0);
            } else {
                // Fallback baseline deal without promotional discount
                bestDealForPlan = new DealBreakdown(
                        plan.price, 0.0, plan.price, "Official", 0.0,
                        0.0, 0.0, null, 0.0, plan.price, plan.price,
                        0.0, 0.0, "STANDARD", "HIGH",
                        new ArrayList<>(), "Standard tariff plan without active promotional coupon.",
                        "provider://recharge", "Recharge in Official App"
                );
            }

            bestDealForPlan.rechargePlan = plan;
            bestDealForPlan.validityDays = plan.validityDays;
            bestDealForPlan.recalculateDailyCosts();
            evaluatedDeals.add(bestDealForPlan);
        }

        if (evaluatedDeals.isEmpty()) {
            return result;
        }

        // 3. Dynamic Ranking based on RechargePreference
        RechargePreference.RankingPriority priority = rechargePref != null ? rechargePref.priority
                : RechargePreference.RankingPriority.BEST_OVERALL_VALUE;
        boolean isValidityOnly = rechargePref != null
                && rechargePref.dataAllowance == RechargePreference.DataAllowance.VALIDITY_ONLY;

        Comparator<DealBreakdown> comparator;

        if (isValidityOnly || priority == RechargePreference.RankingPriority.MAXIMUM_VALIDITY) {
            // Rank by Validity Days DESCENDING, then by lowest daily effective cost ASCENDING
            comparator = (d1, d2) -> {
                int cmp = Integer.compare(d2.validityDays, d1.validityDays);
                if (cmp != 0) return cmp;
                return Double.compare(d1.effectiveCostPerDay, d2.effectiveCostPerDay);
            };
        } else if (priority == RechargePreference.RankingPriority.LOWEST_EFFECTIVE_COST) {
            // Rank by lowest Potential Effective Cost ASCENDING, then by validity DESCENDING
            comparator = (d1, d2) -> {
                int cmp = Double.compare(d1.potentialEffectiveCost, d2.potentialEffectiveCost);
                if (cmp != 0) return cmp;
                return Integer.compare(d2.validityDays, d1.validityDays);
            };
        } else if (priority == RechargePreference.RankingPriority.MAXIMUM_DAILY_DATA) {
            // Rank by Daily Data Allowance DESCENDING
            comparator = (d1, d2) -> {
                double data1 = d1.rechargePlan != null ? d1.rechargePlan.dailyDataGb : 0.0;
                double data2 = d2.rechargePlan != null ? d2.rechargePlan.dailyDataGb : 0.0;
                int cmp = Double.compare(data2, data1);
                if (cmp != 0) return cmp;
                return Double.compare(d1.potentialEffectiveCost, d2.potentialEffectiveCost);
            };
        } else if (priority == RechargePreference.RankingPriority.MAXIMUM_CASHBACK) {
            // Rank by total cashback DESCENDING
            comparator = (d1, d2) -> {
                double cb1 = d1.guaranteedCashback + d1.potentialCashback;
                double cb2 = d2.guaranteedCashback + d2.potentialCashback;
                int cmp = Double.compare(cb2, cb1);
                if (cmp != 0) return cmp;
                return Double.compare(d1.potentialEffectiveCost, d2.potentialEffectiveCost);
            };
        } else {
            // BEST_OVERALL_VALUE (Multi-criteria balanced score)
            comparator = (d1, d2) -> {
                double score1 = calculateValueScore(d1);
                double score2 = calculateValueScore(d2);
                return Double.compare(score2, score1);
            };
        }

        Collections.sort(evaluatedDeals, comparator);

        // Assign explainability tags for why each deal is ranked
        for (DealBreakdown deal : evaluatedDeals) {
            RechargePlanEntity p = deal.rechargePlan;
            if (isValidityOnly || priority == RechargePreference.RankingPriority.MAXIMUM_VALIDITY) {
                deal.whyRecommended = "Recommended for Maximum Validity (" + deal.validityDays + " days) at lowest daily cost of ₹" +
                        String.format(Locale.US, "%.2f", deal.effectiveCostPerDay) + "/day.";
                deal.actionLabel = "VALIDITY SAVER • " + deal.validityDays + " DAYS";
            } else if (priority == RechargePreference.RankingPriority.LOWEST_EFFECTIVE_COST) {
                deal.whyRecommended = "Recommended for Lowest Effective Cost (₹" + (int) deal.potentialEffectiveCost +
                        ") with verified savings of ₹" + (int) (deal.originalMrp - deal.potentialEffectiveCost) + ".";
                deal.actionLabel = "LOWEST COST • ₹" + (int) deal.potentialEffectiveCost;
            } else if (priority == RechargePreference.RankingPriority.MAXIMUM_DAILY_DATA) {
                deal.whyRecommended = "Recommended for High Daily Allowance (" + (p != null ? p.dailyDataGb : 0) + " GB/day).";
                deal.actionLabel = "MAX DATA • " + (p != null ? p.dailyDataGb : 0) + "GB/DAY";
            } else if (priority == RechargePreference.RankingPriority.MAXIMUM_CASHBACK) {
                deal.whyRecommended = "Recommended for Maximum Cashback of ₹" + (int) (deal.guaranteedCashback + deal.potentialCashback) +
                        " via " + deal.recommendedPaymentProvider + ".";
                deal.actionLabel = "MAX CASHBACK • ₹" + (int) (deal.guaranteedCashback + deal.potentialCashback);
            } else {
                deal.whyRecommended = "Best balanced value: " + (p != null && p.dailyDataGb > 0 ? p.dailyDataGb + "GB/day" : "High validity") +
                        ", " + deal.validityDays + " days validity, and ₹" + (int) deal.totalGuaranteedSavings + " assured savings.";
                deal.actionLabel = "BEST OVERALL VALUE";
            }
        }

        result.allRankedDeals = evaluatedDeals;
        result.topDeal = evaluatedDeals.get(0);

        // Compute sub-category flagship recommendations
        result.cheapestGuaranteedDeal = findCheapestGuaranteed(evaluatedDeals);
        result.maximumValidityDeal = findMaxValidity(evaluatedDeals);
        result.bestDataValueDeal = findBestDataValue(evaluatedDeals);
        result.maximumCashbackDeal = findMaxCashback(evaluatedDeals);

        return result;
    }

    private static double calculateValueScore(DealBreakdown deal) {
        RechargePlanEntity p = deal.rechargePlan;
        double validityWeight = deal.validityDays * 1.5;
        double dataWeight = (p != null ? p.dailyDataGb : 0.0) * 35.0;
        double savingsWeight = deal.totalGuaranteedSavings * 1.2;
        double costPenalty = deal.potentialEffectiveCost * 0.35;
        return validityWeight + dataWeight + savingsWeight - costPenalty;
    }

    private static DealBreakdown findCheapestGuaranteed(List<DealBreakdown> deals) {
        DealBreakdown best = null;
        for (DealBreakdown d : deals) {
            if (best == null || d.guaranteedEffectiveCost < best.guaranteedEffectiveCost) {
                best = d;
            }
        }
        return best;
    }

    private static DealBreakdown findMaxValidity(List<DealBreakdown> deals) {
        DealBreakdown best = null;
        for (DealBreakdown d : deals) {
            if (best == null || d.validityDays > best.validityDays) {
                best = d;
            }
        }
        return best;
    }

    private static DealBreakdown findBestDataValue(List<DealBreakdown> deals) {
        DealBreakdown best = null;
        double bestRatio = -1;
        for (DealBreakdown d : deals) {
            double totalData = d.rechargePlan != null ? d.rechargePlan.totalDataGb : 0;
            if (d.potentialEffectiveCost > 0) {
                double ratio = totalData / d.potentialEffectiveCost;
                if (ratio > bestRatio) {
                    bestRatio = ratio;
                    best = d;
                }
            }
        }
        return best != null ? best : deals.get(0);
    }

    private static DealBreakdown findMaxCashback(List<DealBreakdown> deals) {
        DealBreakdown best = null;
        double maxCb = -1;
        for (DealBreakdown d : deals) {
            double cb = d.guaranteedCashback + d.potentialCashback;
            if (cb > maxCb) {
                maxCb = cb;
                best = d;
            }
        }
        return best != null ? best : deals.get(0);
    }
}
