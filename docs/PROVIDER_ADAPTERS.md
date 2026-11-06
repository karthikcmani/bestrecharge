# Provider Inspection Adapters Specification

## 1. Overview
Provider applications employ distinct UI structures, navigation graphs, and view hierarchies. Rather than using fixed screen coordinates or brittle hardcoded selectors, BestRecharge implements dedicated **Provider Navigation Adapters** implementing the `ProviderNavigationWorkflow` interface:

```java
public interface ProviderNavigationWorkflow {
    String getProviderCode();
    String getTargetPackage();
    boolean onScreenChanged(
            AccessibilityService service,
            ProviderInspectionSession session,
            AccessibilityNodeExtractor.ScreenSnapshot snapshot
    );
    void reset();
}
```

## 2. Vodafone Idea (Vi) Adapter (`ViInspectionAdapter`)
- **Target Package:** `com.mventus.selfcare.activity`
- **Primary Workflows:**
  1. **Dashboard:** Locates action buttons containing `"recharge"`, `"prepaid"`, or `"pay bills"`.
  2. **Recharge Plans List:** Traverses hero packs, daily data packs, and validity packs. Extracts price, validity days, voice, and data limits.
  3. **Offers Section:** Navigates to `"View all offers"` or `"Offers"`. Extracts promotional cashback cards (Paytm UPI ₹125, BHIM cashback, Vi App ₹15 discount).
  4. **Terms & Conditions:** Clicks `"Terms & Conditions"` / `"T&C"` dialogs to extract minimum transaction values, expiry dates, and user cohort eligibility rules.
  5. **Auto-Return:** Closes modal dialogs and brings BestRecharge back to the foreground with results.

## 3. Reliance Jio Adapter (`JioInspectionAdapter`)
- **Target Package:** `com.jio.myjio`
- **Primary Workflows:**
  1. Identifies MyJio Mobility recharge dashboard.
  2. Traverses 5G unlimited plans, 1.5GB/day plans, and long-validity (84/365 days) packs.
  3. Extracts partner coupons and cashback offers.

## 4. Bharti Airtel Adapter (`AirtelInspectionAdapter`)
- **Target Package:** `com.myairtelapp`
- **Primary Workflows:**
  1. Identifies Airtel Thanks recharge hub.
  2. Extracts unlimited calling bundles, Airtel Rewards coupons, and payment partner cashbacks.

## 5. BSNL Adapter (`BsnlInspectionAdapter`)
- **Target Package:** `in.co.bsnl`
- **Primary Workflows:**
  1. Identifies BSNL Selfcare official tariff screens.
  2. Collects pure validity packs and low-cost calling tariffs.

## 6. Version Tolerance & Resilient Fallback
Provider applications frequently update their layout hierarchies. The inspection engine implements defensive version tolerance:
- **Semantic Text Matching:** Prefers visible text labels and content descriptions over volatile obfuscated view IDs.
- **Bounded Scrolling:** Caps forward scroll attempts to 2–3 actions to prevent endless loops.
- **Graceful Failure Reporting:** If a screen cannot be classified or navigation stalls, the session reports `"Provider interface changed. Manual inspection required"` without fabricating data or crashing.
