package com.bestrecharge.app.engine;

import com.bestrecharge.app.data.local.entities.RechargePlanEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * PlanFilterEngine applies hard and soft constraint filtering across
 * telecom recharge plans based on user preferences.
 */
public class PlanFilterEngine {

    public static List<RechargePlanEntity> filterPlans(List<RechargePlanEntity> allPlans, RechargePreference pref) {
        if (allPlans == null) return new ArrayList<>();
        if (pref == null) return new ArrayList<>(allPlans);

        List<RechargePlanEntity> filtered = new ArrayList<>();

        for (RechargePlanEntity plan : allPlans) {
            if (plan == null || !plan.isActive) continue;

            // 1. Filter by Operator (if specified and not "ALL")
            if (pref.operatorCode != null && !pref.operatorCode.trim().isEmpty() && !"ALL".equalsIgnoreCase(pref.operatorCode)) {
                if (!pref.operatorCode.equalsIgnoreCase(plan.operatorCode)) {
                    continue;
                }
            }

            // 2. Budget ceiling (strict hard upper limit when maxBudget > 0)
            if (pref.maxBudget > 0 && plan.price > pref.maxBudget) {
                continue;
            }

            // 3. Minimum Validity Days
            if (pref.minValidityDays > 0 && plan.validityDays < pref.minValidityDays) {
                continue;
            }

            // 4. Data Allowance Requirements (mandatory criteria)
            if (pref.dataAllowance != null && pref.dataAllowance != RechargePreference.DataAllowance.ANY) {
                if (pref.dataAllowance == RechargePreference.DataAllowance.VALIDITY_ONLY) {
                    // For validity only, daily data is not required, plans without daily data or with low per-day cost qualify
                } else if (pref.dataAllowance == RechargePreference.DataAllowance.GB_1_DAY) {
                    if (plan.dailyDataGb < 1.0) continue;
                } else if (pref.dataAllowance == RechargePreference.DataAllowance.GB_1_5_DAY) {
                    if (plan.dailyDataGb < 1.5) continue;
                } else if (pref.dataAllowance == RechargePreference.DataAllowance.GB_2_DAY) {
                    if (plan.dailyDataGb < 2.0) continue;
                } else if (pref.dataAllowance == RechargePreference.DataAllowance.GB_2_5_DAY) {
                    if (plan.dailyDataGb < 2.5) continue;
                } else if (pref.dataAllowance == RechargePreference.DataAllowance.GB_3_DAY) {
                    if (plan.dailyDataGb < 3.0) continue;
                }
            }

            // 5. Calling Benefit requirement
            if (pref.unlimitedCallsRequired) {
                String voice = plan.voiceBenefit != null ? plan.voiceBenefit.toLowerCase() : "";
                if (!voice.contains("unlimited")) {
                    continue;
                }
            }

            // 6. OTT Requirement
            if (pref.ottRequired) {
                if (plan.ottBenefits == null || plan.ottBenefits.trim().isEmpty()) {
                    continue;
                }
            }

            filtered.add(plan);
        }

        return filtered;
    }
}
