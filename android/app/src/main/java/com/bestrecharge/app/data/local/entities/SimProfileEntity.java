package com.bestrecharge.app.data.local.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

/**
 * SimProfileEntity stores comprehensive hardware SIM and user-confirmed subscription metadata.
 * It explicitly tracks detection states, distinction between hardware-reported vs manually-entered
 * numbers, and user confirmation to respect Mobile Number Portability (MNP).
 */
@Entity(tableName = "sim_profiles")
public class SimProfileEntity implements Serializable {

    // Telephony Detection States (Section 4)
    public static final String STATE_SIM_NOT_AVAILABLE = "SIM_NOT_AVAILABLE";
    public static final String STATE_NUMBER_UNAVAILABLE = "NUMBER_UNAVAILABLE";
    public static final String STATE_NUMBER_AVAILABLE = "NUMBER_AVAILABLE";
    public static final String STATE_MANUAL_NUMBER_ENTRY = "MANUAL_NUMBER_ENTRY";
    public static final String STATE_USER_CONFIRMED = "USER_CONFIRMED";
    public static final String STATE_OPERATOR_UNVERIFIED = "OPERATOR_UNVERIFIED";
    public static final String STATE_PERMISSION_DENIED = "PERMISSION_DENIED";

    @PrimaryKey
    @NonNull
    public String id;
    public String mobileNumber;
    public String operatorCode; // VI, JIO, AIRTEL, BSNL
    public String operatorName;
    public String circleCode;   // DL, KL, MUM, KA, etc.
    public String circleName;
    public String simSlot;       // SIM 1, SIM 2
    public boolean isPrepaid;
    public boolean isPrimary;

    // Advanced Telephony Attributes
    public int subscriptionId = -1;
    public String carrierName = "";
    public String mccMnc = "";
    public String countryIso = "in";
    public String simState = "UNKNOWN";
    public String detectionState = STATE_NUMBER_UNAVAILABLE;
    public boolean isManuallyEntered = false;
    public boolean isUserConfirmed = false;

    public SimProfileEntity() {
        this.id = "sim_default";
    }

    @androidx.room.Ignore
    public SimProfileEntity(@NonNull String id, String mobileNumber, String operatorCode,
                            String operatorName, String circleCode, String circleName,
                            String simSlot, boolean isPrepaid, boolean isPrimary) {
        this.id = id;
        this.mobileNumber = mobileNumber != null ? mobileNumber : "";
        this.operatorCode = operatorCode;
        this.operatorName = operatorName;
        this.circleCode = circleCode;
        this.circleName = circleName;
        this.simSlot = simSlot;
        this.isPrepaid = isPrepaid;
        this.isPrimary = isPrimary;
        if (this.mobileNumber.isEmpty()) {
            this.detectionState = STATE_NUMBER_UNAVAILABLE;
        } else {
            this.detectionState = STATE_NUMBER_AVAILABLE;
        }
    }

    @androidx.room.Ignore
    public SimProfileEntity(@NonNull String id, String mobileNumber, String operatorCode,
                            String operatorName, String circleCode, String circleName,
                            String simSlot, boolean isPrepaid, boolean isPrimary,
                            int subscriptionId, String carrierName, String mccMnc,
                            String countryIso, String simState, String detectionState,
                            boolean isManuallyEntered, boolean isUserConfirmed) {
        this.id = id;
        this.mobileNumber = mobileNumber != null ? mobileNumber : "";
        this.operatorCode = operatorCode;
        this.operatorName = operatorName;
        this.circleCode = circleCode;
        this.circleName = circleName;
        this.simSlot = simSlot;
        this.isPrepaid = isPrepaid;
        this.isPrimary = isPrimary;
        this.subscriptionId = subscriptionId;
        this.carrierName = carrierName;
        this.mccMnc = mccMnc;
        this.countryIso = countryIso;
        this.simState = simState;
        this.detectionState = detectionState;
        this.isManuallyEntered = isManuallyEntered;
        this.isUserConfirmed = isUserConfirmed;
    }
}
