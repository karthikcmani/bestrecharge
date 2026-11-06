package com.bestrecharge.app.engine;

import java.io.Serializable;

/**
 * RechargePreference encapsulates user requirements for their recharge:
 * data allowance, validity, budget limit, and ranking priority.
 */
public class RechargePreference implements Serializable {

    public enum DataAllowance {
        ANY,
        VALIDITY_ONLY,
        MB_500_DAY,
        GB_1_DAY,
        GB_1_5_DAY,
        GB_2_DAY,
        GB_2_5_DAY,
        GB_3_DAY,
        UNLIMITED_DATA
    }

    public enum RankingPriority {
        BEST_OVERALL_VALUE,
        LOWEST_EFFECTIVE_COST,
        MAXIMUM_VALIDITY,
        MAXIMUM_DAILY_DATA,
        MAXIMUM_CASHBACK
    }

    public DataAllowance dataAllowance = DataAllowance.ANY;
    public int minValidityDays = 0; // 0, 7, 14, 28, 56, 84, 180, 365
    public double maxBudget = 0.0; // 0.0 means unlimited
    public RankingPriority priority = RankingPriority.BEST_OVERALL_VALUE;
    public boolean unlimitedCallsRequired = false;
    public boolean ottRequired = false;
    public String operatorCode = "VI";
    public String circleCode = "DL";

    public RechargePreference() {}

    public RechargePreference(DataAllowance dataAllowance, int minValidityDays, double maxBudget,
                              RankingPriority priority, String operatorCode) {
        this.dataAllowance = dataAllowance;
        this.minValidityDays = minValidityDays;
        this.maxBudget = maxBudget;
        this.priority = priority;
        this.operatorCode = operatorCode;
    }
}
