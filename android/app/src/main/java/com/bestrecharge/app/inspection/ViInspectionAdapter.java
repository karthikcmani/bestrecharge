package com.bestrecharge.app.inspection;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.Rect;
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
 * ViInspectionAdapter coordinates deterministic, multi-tab and deep-scrolling inspection
 * of the Vodafone Idea (Vi) Android application (com.mventus.selfcare.activity).
 *
 * It exhaustively discovers recharge plans, payment offers, app-exclusive discounts,
 * and coupons across multiple tabs without premature back exits.
 */
public class ViInspectionAdapter implements ProviderNavigationWorkflow {

    private static final String TAG = "ViInspectionAdapter";
    public static final String PKG_VI = "com.mventus.selfcare.activity";

    private static final List<String> PLAN_TABS = Arrays.asList(
            "hero unlimited", "truly unlimited", "daily data", "data", "validity", "annual", "unlimited"
    );

    private static final List<String> OFFER_TABS = Arrays.asList(
            "view all offers", "all offers", "upi", "wallets", "bank offers", "coupons", "discounts", "special offers"
    );

    private final Set<String> visitedTabs = new HashSet<>();
    private int scrollAttemptsInCurrentTab = 0;
    private int totalInspectionSteps = 0;
    private long lastActionTime = 0;
    private static final long ACTION_INTERVAL_MS = 600;

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
        visitedTabs.clear();
        scrollAttemptsInCurrentTab = 0;
        totalInspectionSteps = 0;
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
        if (now - lastActionTime < ACTION_INTERVAL_MS) {
            return false;
        }
        lastActionTime = now;
        totalInspectionSteps++;

        // Show/Update floating guidance overlay
        InspectionOverlayManager.getInstance().showOverlay(
                service,
                session.currentStage,
                "Vi App"
        );

        // Security check: halt on sensitive authentication or payment credential screens
        ProviderScreenClassifier.ScreenType screenType = ProviderScreenClassifier.classify(snapshot);
        if (screenType == ProviderScreenClassifier.ScreenType.SENSITIVE_AUTH) {
            InspectionStateManager.getInstance().markSensitiveScreenPaused(session, snapshot.sensitiveReason);
            return true;
        }

        // =========================================================================
        // MODE A: CHECKOUT AUTOMATION
        // =========================================================================
        if (session.isCheckoutMode) {
            return handleCheckoutAutomation(service, session, snapshot);
        }

        // =========================================================================
        // MODE B: FULL TARIFF, OFFER & COUPON INSPECTION
        // =========================================================================

        // 1. Extract and persist all visible plans and offers on every frame
        List<RechargePlanEntity> plans = ScreenContentParser.extractPlans(snapshot, "VI");
        List<OfferEntity> offers = ScreenContentParser.extractOffers(snapshot, "VI");

        if (!plans.isEmpty() || !offers.isEmpty()) {
            OfferExtractionCoordinator.saveInspectedData(service, session, plans, offers, null);
        }

        // Update live progress overlay
        InspectionOverlayManager.getInstance().updateStage(
                "Scanning Vi: " + (plans.size() + offers.size()) + " items on screen. Please do not press Back.",
                session.getCollectedPlansCount(),
                session.getCollectedOffersCount()
        );

        // 2. Handle Terms & Conditions modal if showing
        if (screenType == ProviderScreenClassifier.ScreenType.TERMS_AND_CONDITIONS ||
                isTermsModalShowing(snapshot)) {
            String allText = String.join("\n", snapshot.getAllVisibleTexts());
            OfferExtractionCoordinator.saveInspectedData(service, session, null, null, allText);

            // Close terms modal cleanly via X/Close button - NEVER trigger global BACK!
            AccessibilityNodeExtractor.ExtractedElement closeBtn = findCloseOrXButton(snapshot);
            if (closeBtn != null) {
                clickNodeAt(service, closeBtn);
                return true;
            }
        }

        // 3. If on Home Dashboard, click Recharge or Offers banner to get into plans/offers
        if (screenType == ProviderScreenClassifier.ScreenType.HOME_DASHBOARD) {
            AccessibilityNodeExtractor.ExtractedElement offersBanner = snapshot.findElementContaining("view all offers");
            if (offersBanner == null) offersBanner = snapshot.findElementContaining("offers & rewards");
            if (offersBanner != null) {
                InspectionStateManager.getInstance().updateStage(session,
                        ProviderInspectionSession.Status.NAVIGATING, "Opening Vi offers section...", 25);
                clickNodeAt(service, offersBanner);
                return true;
            }

            AccessibilityNodeExtractor.ExtractedElement rechargeBtn = snapshot.findElementContaining("recharge");
            if (rechargeBtn != null) {
                InspectionStateManager.getInstance().updateStage(session,
                        ProviderInspectionSession.Status.NAVIGATING, "Opening Vi recharge packs...", 30);
                clickNodeAt(service, rechargeBtn);
                return true;
            }
        }

        // 4. If an offers entry link is visible on screen, visit it
        AccessibilityNodeExtractor.ExtractedElement viewAllOffers = snapshot.findElementContaining("view all offers");
        if (viewAllOffers != null && !visitedTabs.contains("view all offers")) {
            visitedTabs.add("view all offers");
            clickNodeAt(service, viewAllOffers);
            return true;
        }

        // 5. Deep Scrolling in current section/tab
        if (scrollAttemptsInCurrentTab < 4) {
            scrollAttemptsInCurrentTab++;
            InspectionStateManager.getInstance().updateStage(session,
                    ProviderInspectionSession.Status.EXTRACTING,
                    "Scrolling list (Page " + (scrollAttemptsInCurrentTab + 1) + "/5)... Please do not press Back.",
                    Math.min(90, 30 + totalInspectionSteps * 3));
            scrollForward(service);
            return true;
        }

        // 6. Switch to next unvisited horizontal tab (Plans or Offers tabs)
        AccessibilityNodeExtractor.ExtractedElement nextTab = findNextUnvisitedTab(snapshot);
        if (nextTab != null) {
            String tabLabel = nextTab.getEffectiveLabel().toLowerCase();
            visitedTabs.add(tabLabel);
            scrollAttemptsInCurrentTab = 0;

            InspectionStateManager.getInstance().updateStage(session,
                    ProviderInspectionSession.Status.NAVIGATING,
                    "Switching to " + nextTab.getEffectiveLabel() + " tab...",
                    Math.min(90, 40 + totalInspectionSteps * 2));

            clickNodeAt(service, nextTab);
            return true;
        }

        // 7. Completion Condition: After exhaustive scrolling & tabs exploration
        if (totalInspectionSteps >= 14 || (session.getCollectedPlansCount() >= 5 && session.getCollectedOffersCount() >= 3)) {
            finishInspection(service, session);
            return true;
        }

        // Final fallback scroll if still under limit
        if (totalInspectionSteps < 20) {
            scrollForward(service);
            return true;
        } else {
            finishInspection(service, session);
            return true;
        }
    }

    private boolean handleCheckoutAutomation(
            AccessibilityService service,
            ProviderInspectionSession session,
            AccessibilityNodeExtractor.ScreenSnapshot snapshot
    ) {
        String allScreenText = String.join(" ", snapshot.getAllVisibleTexts()).toLowerCase();
        int targetAmt = (int) Math.round(session.targetAmount);

        // Check if on Checkout / Payment Page
        boolean onCheckoutPage = allScreenText.contains("amount payable") ||
                allScreenText.contains("payable") ||
                allScreenText.contains("select payment") ||
                allScreenText.contains("view all offers");

        if (onCheckoutPage) {
            session.checkoutPageReached = true;
            String paymentMethod = session.recommendedPayment != null ? session.recommendedPayment : "Paytm / BHIM";
            InspectionOverlayManager.getInstance().updateStage(
                    "✅ Checkout Ready for ₹" + targetAmt + "! Complete via " + paymentMethod + " for maximum savings.",
                    session.getCollectedPlansCount(),
                    session.getCollectedOffersCount()
            );

            // Highlight or scroll to payment provider if needed
            AccessibilityNodeExtractor.ExtractedElement payProvider = snapshot.findElementContaining(paymentMethod.toLowerCase());
            if (payProvider != null) {
                // Keep it in focus
                return true;
            }
            return true;
        }

        // If not yet on checkout, look for the plan amount card (e.g. ₹349)
        AccessibilityNodeExtractor.ExtractedElement planElem = snapshot.findElementContaining("₹" + targetAmt);
        if (planElem == null) {
            planElem = snapshot.findElementContaining(String.valueOf(targetAmt));
        }

        if (planElem != null) {
            InspectionOverlayManager.getInstance().updateStage(
                    "Selecting ₹" + targetAmt + " recharge plan...",
                    session.getCollectedPlansCount(),
                    session.getCollectedOffersCount()
            );
            clickNodeAt(service, planElem);
            return true;
        }

        // Otherwise scroll forward to find the plan
        scrollForward(service);
        return true;
    }

    private void finishInspection(AccessibilityService service, ProviderInspectionSession session) {
        InspectionStateManager.getInstance().markCompleted(session);
        String summary = "Found " + session.getCollectedPlansCount() + " plans and " +
                session.getCollectedOffersCount() + " verified offers & coupons.";

        InspectionOverlayManager.getInstance().showCelebration(summary, () -> {
            bringBestRechargeToForeground(service);
        });
    }

    private boolean isTermsModalShowing(AccessibilityNodeExtractor.ScreenSnapshot snapshot) {
        for (AccessibilityNodeExtractor.ExtractedElement e : snapshot.elements) {
            String label = e.getEffectiveLabel().toLowerCase();
            if (label.contains("terms & conditions") || label.contains("terms and conditions")) {
                return true;
            }
        }
        return false;
    }

    private AccessibilityNodeExtractor.ExtractedElement findCloseOrXButton(AccessibilityNodeExtractor.ScreenSnapshot snapshot) {
        for (AccessibilityNodeExtractor.ExtractedElement e : snapshot.elements) {
            String label = e.getEffectiveLabel().toLowerCase();
            if (label.equals("x") || label.equals("×") || label.equals("close") || label.equals("done")) {
                return e;
            }
        }
        return null;
    }

    private AccessibilityNodeExtractor.ExtractedElement findNextUnvisitedTab(AccessibilityNodeExtractor.ScreenSnapshot snapshot) {
        List<String> allTargetTabs = new ArrayList<>(PLAN_TABS);
        allTargetTabs.addAll(OFFER_TABS);

        for (AccessibilityNodeExtractor.ExtractedElement e : snapshot.elements) {
            String text = e.getEffectiveLabel().toLowerCase().trim();
            for (String target : allTargetTabs) {
                if (text.contains(target) && !visitedTabs.contains(text) && !visitedTabs.contains(target)) {
                    // Only choose elements located in the upper region of the screen (tab bars)
                    if (e.bounds.top > 0 && e.bounds.top < 1200) {
                        return e;
                    }
                }
            }
        }
        return null;
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
                // 1. Try finding scrollable containers
                List<AccessibilityNodeInfo> scrollables = findScrollableContainers(root);
                for (AccessibilityNodeInfo node : scrollables) {
                    if (node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)) {
                        return;
                    }
                }
            }

            // 2. Physical swipe gesture fallback (API 24+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                DisplayMetrics dm = service.getResources().getDisplayMetrics();
                float x = dm.widthPixels / 2f;
                float startY = dm.heightPixels * 0.72f;
                float endY = dm.heightPixels * 0.28f;

                Path path = new Path();
                path.moveTo(x, startY);
                path.lineTo(x, endY);

                GestureDescription.Builder builder = new GestureDescription.Builder();
                builder.addStroke(new GestureDescription.StrokeDescription(path, 50, 350));
                service.dispatchGesture(builder.build(), null, null);
            }
        } catch (Exception ignored) {}
    }

    private List<AccessibilityNodeInfo> findScrollableContainers(AccessibilityNodeInfo root) {
        List<AccessibilityNodeInfo> list = new ArrayList<>();
        if (root == null) return list;
        searchScrollableRecursively(root, list, 0);
        return list;
    }

    private void searchScrollableRecursively(AccessibilityNodeInfo node, List<AccessibilityNodeInfo> list, int depth) {
        if (node == null || depth > 20) return;
        if (node.isScrollable()) {
            list.add(node);
        }
        int count = node.getChildCount();
        for (int i = 0; i < count; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                searchScrollableRecursively(child, list, depth + 1);
            }
        }
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
