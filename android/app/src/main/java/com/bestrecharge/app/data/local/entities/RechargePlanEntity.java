package com.bestrecharge.app.data.local.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "recharge_plans")
public class RechargePlanEntity {
    @PrimaryKey
    @NonNull
    public String id;
    public String operatorCode;
    public String operatorName;
    public String circleCode;
    public double price;
    public int validityDays;
    public double dailyDataGb;
    public double totalDataGb;
    public String voiceBenefit;
    public int smsPerDay;
    public String ottBenefits;
    public String planCategory; // UNLIMITED, DAILY_DATA, LONG_VALIDITY, POPULAR, ANNUAL
    public String description;
    public String source;
    public String lastVerified;
    public boolean isActive;

    public RechargePlanEntity(@NonNull String id, String operatorCode, String operatorName,
                              String circleCode, double price, int validityDays,
                              double dailyDataGb, double totalDataGb, String voiceBenefit,
                              int smsPerDay, String ottBenefits, String planCategory,
                              String description, String source, String lastVerified,
                              boolean isActive) {
        this.id = id;
        this.operatorCode = operatorCode;
        this.operatorName = operatorName;
        this.circleCode = circleCode;
        this.price = price;
        this.validityDays = validityDays;
        this.dailyDataGb = dailyDataGb;
        this.totalDataGb = totalDataGb;
        this.voiceBenefit = voiceBenefit;
        this.smsPerDay = smsPerDay;
        this.ottBenefits = ottBenefits;
        this.planCategory = planCategory;
        this.description = description;
        this.source = source;
        this.lastVerified = lastVerified;
        this.isActive = isActive;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RechargePlanEntity that = (RechargePlanEntity) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
