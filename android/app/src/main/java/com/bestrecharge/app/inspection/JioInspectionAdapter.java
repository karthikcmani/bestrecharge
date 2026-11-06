package com.bestrecharge.app.inspection;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import com.bestrecharge.app.data.local.entities.OfferEntity;
import com.bestrecharge.app.data.local.entities.RechargePlanEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * JioInspectionAdapter coordinates multi-tab, deep-scrolling guided inspection
 * of the Reliance Jio application (com.jio.myjio).
 */
public class JioInspectionAdapter implements ProviderNavigationWorkflow {

    private static final String TAG = "JioInspectionAdapter";
    public static final String PKG_JIO = "com.jio.myjio";

    private static final List<String> JIO_TABS = Arrays.asList(
            "true 5g", "popular plans", "annual", "data packs", "offers", "coupons"
    );

    private final Set<String> visitedTabs = new HashSet<>();
    private int scrollCount = 0;
    private int totalSteps = 0;
    private long lastActionTime = 0;

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
        visitedTabs.clear();
        scrollCount = 0;
        totalSteps = 0;
        lastActionTime = 0;
    }

    @Override
    public boolean onScreenChanged(
            AccessibilityService service,
            ProviderInspectionSession session,
            AccessibilityNodeExtractor.ScreenSnapshot snapshot
    ) {
        if (service == null || session == null || snapshot == null) return false;

        long now = System.currentTimeMillis();
        if (now - lastActionTime < 600) return false;
        lastActionTime = now;
        totalSteps++;

        InspectionOverlayManager.getInstance().showOverlay(service, session.currentStage, "MyJio");

        ProviderScreenClassifier.ScreenType screenType = ProviderScreenClassifier.classify(snapshot);
        if (screenType == ProviderScreenClassifier.ScreenType.SENSITIVE_AUTH) {
            InspectionStateManager.getInstance().markSensitiveScreenPaused(session, snapshot.sensitiveReason);
            return true;
        }

        // Checkout automation mode
        if (session.isCheckoutMode) {
            String allText = String.join(" ", snapshot.getAllVisibleTexts()).toLowerCase();
            int targetAmt = (int) Math.round(session.targetAmount);

            if (allText.contains("payment") || allText.contains("pay") || allText.contains("amount payable")) {
                session.checkoutPageReached = true;
                InspectionOverlayManager.getInstance().updateStage(
                        "✅ Jio Checkout reached for ₹" + targetAmt + "! Choose payment to complete.",
                        session.getCollectedPlansCount(), session.getCollectedOffersCount()
                );
                return true;
            }

            AccessibilityNodeExtractor.ExtractedElement planElem = snapshot.findElementContaining("₹" + targetAmt);
            if (planElem != null) {
                clickNode(service, planElem);
                return true;
            }
            scrollForward(service);
            return true;
        }

        // Extract plans & offers
        List<RechargePlanEntity> plans = ScreenContentParser.extractPlans(snapshot, "JIO");
        List<OfferEntity> offers = ScreenContentParser.extractOffers(snapshot, "JIO");
        if (!plans.isEmpty() || !offers.isEmpty()) {
            OfferExtractionCoordinator.saveInspectedData(service, session, plans, offers, null);
        }

        InspectionOverlayManager.getInstance().updateStage(
                "Scanning MyJio... Please do not press Back.",
                session.getCollectedPlansCount(),
                session.getCollectedOffersCount()
        );

        // Click recharge if on home
        if (screenType == ProviderScreenClassifier.ScreenType.HOME_DASHBOARD) {
            AccessibilityNodeExtractor.ExtractedElement rechargeBtn = snapshot.findElementContaining("recharge");
            if (rechargeBtn != null) {
                clickNode(service, rechargeBtn);
                return true;
            }
        }

        // Scroll current list
        if (scrollCount < 4) {
            scrollCount++;
            scrollForward(service);
            return true;
        }

        // Switch tab
        for (AccessibilityNodeExtractor.ExtractedElement e : snapshot.elements) {
            String txt = e.getEffectiveLabel().toLowerCase();
            for (String t : JIO_TABS) {
                if (txt.contains(t) && !visitedTabs.contains(txt)) {
                    visitedTabs.add(txt);
                    scrollCount = 0;
                    clickNode(service, e);
                    return true;
                }
            }
        }

        // Finish if threshold reached
        if (totalSteps >= 12 || (session.getCollectedPlansCount() >= 5 && session.getCollectedOffersCount() >= 2)) {
            InspectionStateManager.getInstance().markCompleted(session);
            InspectionOverlayManager.getInstance().showCelebration(
                    "Found " + session.getCollectedPlansCount() + " Jio plans & " + session.getCollectedOffersCount() + " offers!",
                    () -> bringBestRechargeToForeground(service)
            );
            return true;
        }

        scrollForward(service);
        return true;
    }

    private void clickNode(AccessibilityService service, AccessibilityNodeExtractor.ExtractedElement elem) {
        if (service == null || elem == null) return;
        try {
            AccessibilityNodeInfo root = service.getRootInActiveWindow();
            if (root == null) return;
            List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByText(elem.getEffectiveLabel());
            if (nodes != null && !nodes.isEmpty()) {
                nodes.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
        } catch (Exception ignored) {}
    }

    private void scrollForward(AccessibilityService service) {
        if (service == null) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                DisplayMetrics dm = service.getResources().getDisplayMetrics();
                Path path = new Path();
                path.moveTo(dm.widthPixels / 2f, dm.heightPixels * 0.72f);
                path.lineTo(dm.widthPixels / 2f, dm.heightPixels * 0.28f);
                GestureDescription.Builder b = new GestureDescription.Builder();
                b.addStroke(new GestureDescription.StrokeDescription(path, 50, 350));
                service.dispatchGesture(b.build(), null, null);
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
