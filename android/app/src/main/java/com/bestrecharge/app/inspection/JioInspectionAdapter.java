package com.bestrecharge.app.inspection;

import android.accessibilityservice.AccessibilityService;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import com.bestrecharge.app.data.local.entities.OfferEntity;
import com.bestrecharge.app.data.local.entities.RechargePlanEntity;

import java.util.List;

/**
 * JioInspectionAdapter coordinates guided inspection of the Reliance Jio application (com.jio.myjio).
 */
public class JioInspectionAdapter implements ProviderNavigationWorkflow {

    private static final String TAG = "JioInspectionAdapter";
    public static final String PKG_JIO = "com.jio.myjio";

    private boolean plansRead = false;

    @Override
    public String getProviderCode() {
        return "JIO";
    }

    @Override
    public String getTargetPackage() {
        return PKG_JIO;
    }

    @Override
    public void reset() {
        plansRead = false;
    }

    @Override
    public boolean onScreenChanged(
            AccessibilityService service,
            ProviderInspectionSession session,
            AccessibilityNodeExtractor.ScreenSnapshot snapshot
    ) {
        if (service == null || session == null || snapshot == null) return false;

        ProviderScreenClassifier.ScreenType screenType = ProviderScreenClassifier.classify(snapshot);
        Log.d(TAG, "Jio screen classified as: " + screenType);

        if (screenType == ProviderScreenClassifier.ScreenType.SENSITIVE_AUTH) {
            InspectionStateManager.getInstance().markSensitiveScreenPaused(session, snapshot.sensitiveReason);
            return true;
        }

        if (!plansRead) {
            InspectionStateManager.getInstance().updateStage(session,
                    ProviderInspectionSession.Status.EXTRACTING,
                    "Reading Jio recharge plans & coupons...", 50);

            List<RechargePlanEntity> plans = ScreenContentParser.extractPlans(snapshot, "JIO");
            List<OfferEntity> offers = ScreenContentParser.extractOffers(snapshot, "JIO");

            if (!plans.isEmpty() || !offers.isEmpty()) {
                OfferExtractionCoordinator.saveInspectedData(service, session, plans, offers, null);
                plansRead = true;
                InspectionStateManager.getInstance().markCompleted(session);
                bringBestRechargeToForeground(service);
                return true;
            }

            // Click "Recharge" button if on Home
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
