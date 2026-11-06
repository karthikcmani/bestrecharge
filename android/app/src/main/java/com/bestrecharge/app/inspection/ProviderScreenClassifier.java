package com.bestrecharge.app.inspection;

import java.util.List;

/**
 * ProviderScreenClassifier identifies the semantic role of the active telecom screen
 * based on extracted text patterns, resource IDs, and actionable elements.
 */
public class ProviderScreenClassifier {

    public enum ScreenType {
        HOME_DASHBOARD,
        RECHARGE_PLANS,
        OFFERS_SECTION,
        TERMS_AND_CONDITIONS,
        SENSITIVE_AUTH,
        POPUP_DIALOG,
        UNKNOWN
    }

    public static ScreenType classify(AccessibilityNodeExtractor.ScreenSnapshot snapshot) {
        if (snapshot == null) return ScreenType.UNKNOWN;

        if (snapshot.isSensitiveAuthScreen) {
            return ScreenType.SENSITIVE_AUTH;
        }

        List<String> texts = snapshot.getAllVisibleTexts();
        StringBuilder sb = new StringBuilder();
        for (String t : texts) {
            sb.append(t.toLowerCase()).append(" ");
        }
        String content = sb.toString();

        // 1. Check Terms & Conditions
        if (content.contains("terms & conditions") || content.contains("terms and conditions") ||
                (content.contains("offer valid") && content.contains("cashback")) ||
                content.contains("min transaction value") || content.contains("minimum transaction")) {
            return ScreenType.TERMS_AND_CONDITIONS;
        }

        // 2. Check Offers Section
        if (content.contains("view all offers") || content.contains("available offers") ||
                content.contains("payment offers") || content.contains("apply coupon") ||
                (content.contains("cashback") && (content.contains("paytm") || content.contains("bhim") || content.contains("phonepe")))) {
            return ScreenType.OFFERS_SECTION;
        }

        // 3. Check Recharge Plans List
        if (content.contains("hero unlimited") || content.contains("daily data") ||
                content.contains("validity:") || content.contains("truly unlimited") ||
                content.contains("recommended packs") || (content.contains("gb/day") && content.contains("days"))) {
            return ScreenType.RECHARGE_PLANS;
        }

        // 4. Check Home / Dashboard
        if (content.contains("active pack") || content.contains("data left") ||
                content.contains("recharge for") || content.contains("my account") ||
                content.contains("switch account") || content.contains("pay bills")) {
            return ScreenType.HOME_DASHBOARD;
        }

        return ScreenType.UNKNOWN;
    }
}
