package com.bestrecharge.app.inspection;

import android.accessibilityservice.AccessibilityService;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import com.bestrecharge.app.data.local.entities.OfferEntity;
import com.bestrecharge.app.data.local.entities.RechargePlanEntity;

import java.util.List;

/**
 * AirtelInspectionAdapter coordinates guided inspection of the Airtel Thanks app (com.myairtelapp).
 */
public class AirtelInspectionAdapter implements ProviderNavigationWorkflow {

    private static final String TAG = "AirtelInspectionAdapter";
    public static final String PKG_AIRTEL = "com.myairtelapp";

    private boolean completed = false;

    @Override
    public String getProviderCode() {
        return "AIRTEL";
    }

    @Override
    public String getTargetPackage() {
        return PKG_AIRTEL;
    }

    @Override
    public void reset() {
        completed = false;
    }

    @Override
    public boolean onScreenChanged(
            AccessibilityService service,
            ProviderInspectionSession session,
            AccessibilityNodeExtractor.ScreenSnapshot snapshot
    ) {
        if (service == null || session == null || snapshot == null) return false;

        if (snapshot.isSensitiveAuthScreen) {
            InspectionStateManager.getInstance().markSensitiveScreenPaused(session, snapshot.sensitiveReason);
            return true;
        }

        if (!completed) {
            InspectionStateManager.getInstance().updateStage(session,
                    ProviderInspectionSession.Status.EXTRACTING,
                    "Reading Airtel plans & payment cashback...", 50);

            List<RechargePlanEntity> plans = ScreenContentParser.extractPlans(snapshot, "AIRTEL");
            List<OfferEntity> offers = ScreenContentParser.extractOffers(snapshot, "AIRTEL");

            if (!plans.isEmpty() || !offers.isEmpty()) {
                OfferExtractionCoordinator.saveInspectedData(service, session, plans, offers, null);
                completed = true;
                InspectionStateManager.getInstance().markCompleted(session);
                bringBestRechargeToForeground(service);
                return true;
            }

            AccessibilityNodeExtractor.ExtractedElement rechargeBtn = snapshot.findElementContaining("recharge");
            if (rechargeBtn != null) {
                clickNode(service, rechargeBtn.getEffectiveLabel());
                return true;
            }
        }

        return false;
    }

    private void clickNode(AccessibilityService service, String text) {
        try {
            AccessibilityNodeInfo root = service.getRootInActiveWindow();
            if (root == null) return;
            List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByText(text);
            if (nodes != null && !nodes.isEmpty()) {
                nodes.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
        } catch (Exception ignored) {}
    }

    private void bringBestRechargeToForeground(AccessibilityService service) {
        try {
            android.content.Intent launchIntent = service.getPackageManager()
                    .getLaunchIntentForPackage(service.getPackageName());
            if (launchIntent != null) {
                launchIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK |
                        android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP);
                service.startActivity(launchIntent);
            }
        } catch (Exception ignored) {}
    }
}
