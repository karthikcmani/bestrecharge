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

public class SimDetectorManager {

    public interface OnSimDetectedListener {
        void onSimsDetected(List<SimProfileEntity> detectedSims);
        void onPermissionNeeded();
        void onError(String message);
    }

    public static boolean hasTelephonyPermissions(Context context) {
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
        if (!hasTelephonyPermissions(context)) {
            listener.onPermissionNeeded();
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                SubscriptionManager subscriptionManager = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
                if (subscriptionManager == null) {
                    listener.onError("SubscriptionManager unavailable on device");
                    return;
                }

                List<SubscriptionInfo> subscriptionInfoList = null;
                try {
                    subscriptionInfoList = subscriptionManager.getActiveSubscriptionInfoList();
                } catch (SecurityException se) {
                    listener.onPermissionNeeded();
                    return;
                }

                if (subscriptionInfoList == null || subscriptionInfoList.isEmpty()) {
                    // Fallback to TelephonyManager for basic SIM status
                    TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                    String networkOperatorName = tm != null ? tm.getNetworkOperatorName() : "Unknown";
                    String simOperatorName = tm != null ? tm.getSimOperatorName() : networkOperatorName;

                    String opCode = mapCarrierToCode(simOperatorName);
                    String opName = mapCarrierToFullName(opCode);

                    SimProfileEntity singleSim = new SimProfileEntity(
                            "sim_device_1",
                            "",
                            opCode,
                            opName,
                            "DL",
                            "Delhi NCR",
                            "SIM 1",
                            true,
                            true
                    );
                    List<SimProfileEntity> list = new ArrayList<>();
                    list.add(singleSim);
                    persistSims(context, list);
                    listener.onSimsDetected(list);
                    return;
                }

                List<SimProfileEntity> detectedList = new ArrayList<>();
                for (int i = 0; i < subscriptionInfoList.size(); i++) {
                    SubscriptionInfo info = subscriptionInfoList.get(i);
                    int slotIndex = info.getSimSlotIndex();
                    String slotLabel = "SIM " + (slotIndex >= 0 ? (slotIndex + 1) : (i + 1));

                    CharSequence carrierNameCS = info.getCarrierName();
                    if (carrierNameCS == null || carrierNameCS.toString().trim().isEmpty()) {
                        carrierNameCS = info.getDisplayName();
                    }
                    String rawCarrier = carrierNameCS != null ? carrierNameCS.toString() : "Vi";
                    String opCode = mapCarrierToCode(rawCarrier);
                    String opName = mapCarrierToFullName(opCode);

                    String number = "";
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            number = subscriptionManager.getPhoneNumber(info.getSubscriptionId());
                        } else {
                            number = info.getNumber();
                        }
                    } catch (Exception ignored) {}

                    if (number != null) {
                        number = number.replace("+91", "").replace(" ", "").trim();
                    } else {
                        number = "";
                    }

                    boolean isPrimary = (i == 0);
                    SimProfileEntity simProfile = new SimProfileEntity(
                            "sim_device_" + (i + 1),
                            number,
                            opCode,
                            opName,
                            "DL",
                            "Delhi NCR",
                            slotLabel,
                            true,
                            isPrimary
                    );
                    detectedList.add(simProfile);
                }

                persistSims(context, detectedList);
                listener.onSimsDetected(detectedList);

            } catch (Exception e) {
                listener.onError("SIM detection error: " + e.getMessage());
            }
        });
    }

    private static void persistSims(Context context, List<SimProfileEntity> sims) {
        AppDatabase db = AppDatabase.getInstance(context);
        for (SimProfileEntity s : sims) {
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
