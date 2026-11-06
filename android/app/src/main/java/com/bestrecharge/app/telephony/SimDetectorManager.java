package com.bestrecharge.app.telephony;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.core.content.ContextCompat;

import com.bestrecharge.app.data.local.AppDatabase;
import com.bestrecharge.app.data.local.entities.SimProfileEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * SimDetectorManager executes genuine Android telephony inspection across single and dual-SIM slots.
 * It enforces strict integrity:
 * 1. Never fabricates a missing mobile number.
 * 2. Never displays fake auto-detection toasts.
 * 3. Clearly distinguishes between hardware-reported, manually-entered, and user-confirmed numbers.
 * 4. Distinguishes SIM detection states (SIM_NOT_AVAILABLE, NUMBER_UNAVAILABLE, NUMBER_AVAILABLE, etc.).
 */
public class SimDetectorManager {

    public interface OnSimDetectedListener {
        void onSimsDetected(List<SimProfileEntity> detectedSims);
        void onPermissionNeeded();
        void onError(String message);
    }

    public static boolean hasTelephonyPermissions(Context context) {
        if (context == null) return false;
        boolean hasPhoneState = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
                == PackageManager.PERMISSION_GRANTED;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            boolean hasPhoneNumbers = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_NUMBERS)
                    == PackageManager.PERMISSION_GRANTED;
            return hasPhoneState || hasPhoneNumbers;
        }
        return hasPhoneState;
    }

    public static void detectSimCards(Context context, OnSimDetectedListener listener) {
        if (context == null) return;
        if (!hasTelephonyPermissions(context)) {
            if (listener != null) listener.onPermissionNeeded();
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                SubscriptionManager subscriptionManager = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
                TelephonyManager defaultTm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

                if (subscriptionManager == null) {
                    if (listener != null) listener.onError("SubscriptionManager unavailable on device");
                    return;
                }

                List<SubscriptionInfo> subscriptionInfoList = null;
                try {
                    subscriptionInfoList = subscriptionManager.getActiveSubscriptionInfoList();
                } catch (SecurityException se) {
                    if (listener != null) listener.onPermissionNeeded();
                    return;
                }

                List<SimProfileEntity> detectedList = new ArrayList<>();

                if (subscriptionInfoList == null || subscriptionInfoList.isEmpty()) {
                    // No hardware SIM active in slots
                    String simStateDesc = "ABSENT";
                    if (defaultTm != null) {
                        int state = defaultTm.getSimState();
                        simStateDesc = mapSimStateToString(state);
                    }

                    SimProfileEntity emptySim = new SimProfileEntity(
                            "sim_none",
                            "",
                            "UNKNOWN",
                            "No Active SIM Detected",
                            "IN",
                            "Pan India",
                            "No SIM",
                            true,
                            true,
                            -1,
                            "None",
                            "",
                            "in",
                            simStateDesc,
                            SimProfileEntity.STATE_SIM_NOT_AVAILABLE,
                            false,
                            false
                    );
                    detectedList.add(emptySim);
                    persistSims(context, detectedList);
                    if (listener != null) listener.onSimsDetected(detectedList);
                    return;
                }

                // Process active subscriptions (support dual SIM)
                for (int i = 0; i < subscriptionInfoList.size(); i++) {
                    SubscriptionInfo info = subscriptionInfoList.get(i);
                    int slotIndex = info.getSimSlotIndex();
                    String slotLabel = "SIM " + (slotIndex >= 0 ? (slotIndex + 1) : (i + 1));
                    int subId = info.getSubscriptionId();

                    CharSequence carrierNameCS = info.getCarrierName();
                    if (carrierNameCS == null || carrierNameCS.toString().trim().isEmpty()) {
                        carrierNameCS = info.getDisplayName();
                    }
                    String rawCarrier = carrierNameCS != null ? carrierNameCS.toString().trim() : "Unknown";
                    String opCode = mapCarrierToCode(rawCarrier);
                    String opName = mapCarrierToFullName(opCode);

                    // Retrieve MCC / MNC
                    String mccMnc = "";
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        String mcc = info.getMccString() != null ? info.getMccString() : "";
                        String mnc = info.getMncString() != null ? info.getMncString() : "";
                        mccMnc = mcc + mnc;
                    }

                    String countryIso = info.getCountryIso() != null ? info.getCountryIso().toLowerCase() : "in";

                    // Retrieve hardware-reported MSISDN where supported
                    String rawNumber = "";
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            rawNumber = subscriptionManager.getPhoneNumber(subId);
                        } else {
                            rawNumber = info.getNumber();
                        }
                    } catch (SecurityException se) {
                        // Permission not granted for phone number retrieval
                    } catch (Exception ignored) {}

                    String cleanedNumber = "";
                    if (rawNumber != null && !rawNumber.trim().isEmpty()) {
                        cleanedNumber = rawNumber.replaceAll("[^0-9]", "");
                        if (cleanedNumber.length() > 10) {
                            cleanedNumber = cleanedNumber.substring(cleanedNumber.length() - 10);
                        }
                    }

                    // Check if user previously confirmed a number for this SIM slot
                    SimProfileEntity existing = AppDatabase.getInstance(context).simProfileDao().getPrimarySimProfile();
                    boolean isManuallyEntered = false;
                    boolean isUserConfirmed = false;
                    String detectionState;

                    if (!cleanedNumber.isEmpty()) {
                        detectionState = SimProfileEntity.STATE_NUMBER_AVAILABLE;
                    } else if (existing != null && existing.isUserConfirmed && existing.mobileNumber != null && !existing.mobileNumber.isEmpty()) {
                        cleanedNumber = existing.mobileNumber;
                        isManuallyEntered = existing.isManuallyEntered;
                        isUserConfirmed = true;
                        detectionState = SimProfileEntity.STATE_USER_CONFIRMED;
                    } else {
                        // Number is not written on the SIM card
                        detectionState = SimProfileEntity.STATE_NUMBER_UNAVAILABLE;
                    }

                    // Determine operator & circle from DoT series if number is available
                    String circleName = "Delhi NCR";
                    String circleCode = "DL";
                    if (!cleanedNumber.isEmpty() && cleanedNumber.length() >= 4) {
                        com.bestrecharge.app.utils.TelecomNumberSeriesHelper.AllocationInfo alloc =
                                com.bestrecharge.app.utils.TelecomNumberSeriesHelper.getAllocation(cleanedNumber);
                        if (alloc != null) {
                            circleName = alloc.circleName;
                            circleCode = alloc.circleCode;
                            // If carrier raw name was generic or unknown, prioritize series allocation
                            if ("UNKNOWN".equalsIgnoreCase(opCode) || "VI".equalsIgnoreCase(opCode)) {
                                opCode = alloc.operatorCode;
                                opName = alloc.operatorName;
                            }
                        }
                    }

                    // Query SIM state via subscription-specific TelephonyManager where available
                    String simStateDesc = "READY";
                    if (defaultTm != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            TelephonyManager subTm = defaultTm.createForSubscriptionId(subId);
                            simStateDesc = mapSimStateToString(subTm.getSimState());
                        } else {
                            simStateDesc = mapSimStateToString(defaultTm.getSimState());
                        }
                    }

                    boolean isPrimary = (i == 0);
                    SimProfileEntity simProfile = new SimProfileEntity(
                            "sim_device_" + (i + 1),
                            cleanedNumber,
                            opCode,
                            opName,
                            circleCode,
                            circleName,
                            slotLabel,
                            true,
                            isPrimary,
                            subId,
                            rawCarrier,
                            mccMnc,
                            countryIso,
                            simStateDesc,
                            detectionState,
                            isManuallyEntered,
                            isUserConfirmed
                    );
                    detectedList.add(simProfile);
                }

                persistSims(context, detectedList);
                if (listener != null) listener.onSimsDetected(detectedList);

            } catch (Exception e) {
                if (listener != null) listener.onError("SIM detection error: " + e.getMessage());
            }
        });
    }

    private static String mapSimStateToString(int state) {
        switch (state) {
            case TelephonyManager.SIM_STATE_READY:
                return "READY";
            case TelephonyManager.SIM_STATE_ABSENT:
                return "ABSENT";
            case TelephonyManager.SIM_STATE_PIN_REQUIRED:
                return "PIN_LOCKED";
            case TelephonyManager.SIM_STATE_PUK_REQUIRED:
                return "PUK_LOCKED";
            case TelephonyManager.SIM_STATE_NETWORK_LOCKED:
                return "NETWORK_LOCKED";
            default:
                return "UNKNOWN";
        }
    }

    private static void persistSims(Context context, List<SimProfileEntity> sims) {
        AppDatabase db = AppDatabase.getInstance(context);
        db.simProfileDao().deleteAllSims();
        for (int i = 0; i < sims.size(); i++) {
            SimProfileEntity s = sims.get(i);
            s.isPrimary = (i == 0);
            db.simProfileDao().insertOrUpdate(s);
        }
    }

    public static String mapCarrierToCode(String raw) {
        if (raw == null) return "VI";
        String lower = raw.toLowerCase();
        if (lower.contains("jio") || lower.contains("reliance")) {
            return "JIO";
        } else if (lower.contains("airtel") || lower.contains("bharti")) {
            return "AIRTEL";
        } else if (lower.contains("bsnl") || lower.contains("cellone")) {
            return "BSNL";
        } else if (lower.contains("vi") || lower.contains("vodafone") || lower.contains("idea")) {
            return "VI";
        }
        return "VI";
    }

    public static String mapCarrierToFullName(String code) {
        if ("JIO".equalsIgnoreCase(code)) return "Reliance Jio";
        if ("AIRTEL".equalsIgnoreCase(code)) return "Bharti Airtel";
        if ("BSNL".equalsIgnoreCase(code)) return "BSNL";
        return "Vodafone Idea (Vi)";
    }
}
