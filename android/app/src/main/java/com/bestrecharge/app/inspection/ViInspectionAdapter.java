package com.bestrecharge.app.inspection;

import android.accessibilityservice.AccessibilityService;
import android.os.Build;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import com.bestrecharge.app.data.local.entities.OfferEntity;
import com.bestrecharge.app.data.local.entities.RechargePlanEntity;

import java.util.List;

/**
 * ViInspectionAdapter coordinates deterministic, version-tolerant inspection of the Vodafone Idea (Vi)
 * Android application (com.mventus.selfcare.activity).
 */
public class ViInspectionAdapter implements ProviderNavigationWorkflow {

    private static final String TAG = "ViInspectionAdapter";
    public static final String PKG_VI = "com.mventus.selfcare.activity";

    private enum Step {
        LOCATE_RECHARGE,
        READ_PLANS,
        OPEN_OFFERS,
        READ_OFFERS,
        READ_TERMS,
        FINISHED
    }

    private Step currentStep = Step.LOCATE_RECHARGE;
    private int scrollAttempts = 0;

    @Override
    public String getProviderCode() {
        return "VI";
    }

    @Override
    public String getTargetPackage() {
        return PKG_VI;
    }

    @Override
    public void reset() {
        currentStep = Step.LOCATE_RECHARGE;
        scrollAttempts = 0;
    }

    @Override
    public boolean onScreenChanged(
            AccessibilityService service,
            ProviderInspectionSession session,
            AccessibilityNodeExtractor.ScreenSnapshot snapshot
    ) {
        if (service == null || session == null || snapshot == null) return false;

        ProviderScreenClassifier.ScreenType screenType = ProviderScreenClassifier.classify(snapshot);
        Log.d(TAG, "Vi screen classified as: " + screenType + ", current step: " + currentStep);

        if (screenType == ProviderScreenClassifier.ScreenType.SENSITIVE_AUTH) {
            InspectionStateManager.getInstance().markSensitiveScreenPaused(session, snapshot.sensitiveReason);
            return true;
        }

        switch (currentStep) {
            case LOCATE_RECHARGE:
                InspectionStateManager.getInstance().updateStage(session,
                        ProviderInspectionSession.Status.NAVIGATING,
                        "Locating Vi recharge section...", 20);

                if (screenType == ProviderScreenClassifier.ScreenType.RECHARGE_PLANS) {
                    currentStep = Step.READ_PLANS;
                    return onScreenChanged(service, session, snapshot);
                }

                // Look for "Recharge" or "Prepaid" button
                AccessibilityNodeExtractor.ExtractedElement rechargeBtn = snapshot.findElementContaining("recharge");
                if (rechargeBtn != null) {
                    clickNodeAt(service, rechargeBtn);
                    currentStep = Step.READ_PLANS;
                    return true;
                }
                break;

            case READ_PLANS:
                InspectionStateManager.getInstance().updateStage(session,
                        ProviderInspectionSession.Status.EXTRACTING,
                        "Extracting Vi recharge plans & offers...", 45);

                List<RechargePlanEntity> plans = ScreenContentParser.extractPlans(snapshot, "VI");
                if (!plans.isEmpty()) {
                    OfferExtractionCoordinator.saveInspectedData(service, session, plans, null, null);
                }

                // Look for "Offers" or "View All Offers"
                AccessibilityNodeExtractor.ExtractedElement offersBtn = snapshot.findElementContaining("view all offers");
                if (offersBtn == null) {
                    offersBtn = snapshot.findElementContaining("offers");
                }
                if (offersBtn != null) {
                    clickNodeAt(service, offersBtn);
                    currentStep = Step.READ_OFFERS;
                    return true;
                } else if (scrollAttempts < 2) {
                    scrollAttempts++;
                    scrollForward(service);
                    return true;
                } else {
                    currentStep = Step.READ_OFFERS;
                }
                break;

            case READ_OFFERS:
                InspectionStateManager.getInstance().updateStage(session,
                        ProviderInspectionSession.Status.EXTRACTING,
                        "Reading payment offers & discounts...", 70);

                List<OfferEntity> offers = ScreenContentParser.extractOffers(snapshot, "VI");
                if (!offers.isEmpty()) {
                    OfferExtractionCoordinator.saveInspectedData(service, session, null, offers, null);
                }

                // Look for Terms & Conditions
                AccessibilityNodeExtractor.ExtractedElement tcBtn = snapshot.findElementContaining("terms");
                if (tcBtn == null) {
                    tcBtn = snapshot.findElementContaining("t&c");
                }
                if (tcBtn != null) {
                    clickNodeAt(service, tcBtn);
                    currentStep = Step.READ_TERMS;
                    return true;
                } else {
                    currentStep = Step.FINISHED;
                }
                break;

            case READ_TERMS:
                InspectionStateManager.getInstance().updateStage(session,
                        ProviderInspectionSession.Status.TERMS_INSPECTION,
                        "Extracting promotional terms & conditions...", 85);

                String allText = String.join("\n", snapshot.getAllVisibleTexts());
                OfferExtractionCoordinator.saveInspectedData(service, session, null, null, allText);

                // Close terms dialog by clicking X or back
                AccessibilityNodeExtractor.ExtractedElement closeBtn = snapshot.findElementContaining("close");
                if (closeBtn != null) {
                    clickNodeAt(service, closeBtn);
                } else {
                    service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
                }
                currentStep = Step.FINISHED;
                return true;

            case FINISHED:
                InspectionStateManager.getInstance().markCompleted(session);
                // Return to BestRecharge
                bringBestRechargeToForeground(service);
                return true;
        }

        return false;
    }

    private void clickNodeAt(AccessibilityService service, AccessibilityNodeExtractor.ExtractedElement elem) {
        if (service == null || elem == null) return;
        try {
            AccessibilityNodeInfo root = service.getRootInActiveWindow();
            if (root == null) return;

            List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByText(elem.getEffectiveLabel());
            if (nodes != null && !nodes.isEmpty()) {
                for (AccessibilityNodeInfo node : nodes) {
                    if (node.isClickable()) {
                        node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        return;
                    }
                    AccessibilityNodeInfo parent = node.getParent();
                    while (parent != null) {
                        if (parent.isClickable()) {
                            parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            return;
                        }
                        parent = parent.getParent();
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Click node failed: " + e.getMessage());
        }
    }

    private void scrollForward(AccessibilityService service) {
        if (service == null) return;
        try {
            AccessibilityNodeInfo root = service.getRootInActiveWindow();
            if (root != null) {
                root.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
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
