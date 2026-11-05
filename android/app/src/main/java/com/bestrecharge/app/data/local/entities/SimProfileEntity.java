package com.bestrecharge.app.data.local.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "sim_profiles")
public class SimProfileEntity {
    @PrimaryKey
    @NonNull
    public String id;
    public String mobileNumber;
    public String operatorCode; // VI, JIO, AIRTEL, BSNL
    public String operatorName;
    public String circleCode;   // DL, MUM, KA, etc.
    public String circleName;
    public String simSlot;       // SIM 1, SIM 2
    public boolean isPrepaid;
    public boolean isPrimary;

    public SimProfileEntity(@NonNull String id, String mobileNumber, String operatorCode,
                            String operatorName, String circleCode, String circleName,
                            String simSlot, boolean isPrepaid, boolean isPrimary) {
        this.id = id;
        this.mobileNumber = mobileNumber;
        this.operatorCode = operatorCode;
        this.operatorName = operatorName;
        this.circleCode = circleCode;
        this.circleName = circleName;
        this.simSlot = simSlot;
        this.isPrepaid = isPrepaid;
        this.isPrimary = isPrimary;
    }
}
