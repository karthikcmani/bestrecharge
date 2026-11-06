package com.bestrecharge.app.inspection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.bestrecharge.app.data.local.entities.OfferEntity;
import com.bestrecharge.app.data.local.entities.RechargePlanEntity;
import com.bestrecharge.app.data.local.entities.SimProfileEntity;

import org.junit.Test;

import java.util.List;

/**
 * Unit tests verifying ProviderScreenClassifier, ScreenContentParser,
 * sensitive screen protections, and SIM detection states.
 */
public class ProviderInspectionEngineTest {

    @Test
    public void testClassifyHomeScreen() {
        AccessibilityNodeExtractor.ScreenSnapshot snapshot = new AccessibilityNodeExtractor.ScreenSnapshot();
        snapshot.packageName = "com.mventus.selfcare.activity";

        AccessibilityNodeExtractor.ExtractedElement e1 = new AccessibilityNodeExtractor.ExtractedElement();
        e1.text = "Active Pack: Unlimited Calls + 1.5GB/day";
        snapshot.elements.add(e1);

        AccessibilityNodeExtractor.ExtractedElement e2 = new AccessibilityNodeExtractor.ExtractedElement();
        e2.text = "Recharge Now";
        e2.isClickable = true;
        snapshot.elements.add(e2);

        ProviderScreenClassifier.ScreenType type = ProviderScreenClassifier.classify(snapshot);
        assertEquals(ProviderScreenClassifier.ScreenType.HOME_DASHBOARD, type);
    }

    @Test
    public void testClassifyRechargePlansScreen() {
        AccessibilityNodeExtractor.ScreenSnapshot snapshot = new AccessibilityNodeExtractor.ScreenSnapshot();
        snapshot.packageName = "com.mventus.selfcare.activity";

        AccessibilityNodeExtractor.ExtractedElement e1 = new AccessibilityNodeExtractor.ExtractedElement();
        e1.text = "Hero Unlimited Packs";
        snapshot.elements.add(e1);

        AccessibilityNodeExtractor.ExtractedElement e2 = new AccessibilityNodeExtractor.ExtractedElement();
        e2.text = "₹349 • 28 Days • 1.5 GB/day";
        snapshot.elements.add(e2);

        ProviderScreenClassifier.ScreenType type = ProviderScreenClassifier.classify(snapshot);
        assertEquals(ProviderScreenClassifier.ScreenType.RECHARGE_PLANS, type);
    }

    @Test
    public void testClassifyOffersScreen() {
        AccessibilityNodeExtractor.ScreenSnapshot snapshot = new AccessibilityNodeExtractor.ScreenSnapshot();
        snapshot.packageName = "com.mventus.selfcare.activity";

        AccessibilityNodeExtractor.ExtractedElement e1 = new AccessibilityNodeExtractor.ExtractedElement();
        e1.text = "View All Offers & Coupons";
        snapshot.elements.add(e1);

        AccessibilityNodeExtractor.ExtractedElement e2 = new AccessibilityNodeExtractor.ExtractedElement();
        e2.text = "Paytm UPI Assured Cashback Up to ₹125";
        snapshot.elements.add(e2);

        ProviderScreenClassifier.ScreenType type = ProviderScreenClassifier.classify(snapshot);
        assertEquals(ProviderScreenClassifier.ScreenType.OFFERS_SECTION, type);
    }

    @Test
    public void testClassifyTermsScreen() {
        AccessibilityNodeExtractor.ScreenSnapshot snapshot = new AccessibilityNodeExtractor.ScreenSnapshot();
        snapshot.packageName = "com.mventus.selfcare.activity";

        AccessibilityNodeExtractor.ExtractedElement e1 = new AccessibilityNodeExtractor.ExtractedElement();
        e1.text = "terms & conditions";
        snapshot.elements.add(e1);

        AccessibilityNodeExtractor.ExtractedElement e2 = new AccessibilityNodeExtractor.ExtractedElement();
        e2.text = "Offer valid from 1st to 30th November 2026. Min transaction value of ₹99.";
        snapshot.elements.add(e2);

        ProviderScreenClassifier.ScreenType type = ProviderScreenClassifier.classify(snapshot);
        assertEquals(ProviderScreenClassifier.ScreenType.TERMS_AND_CONDITIONS, type);
    }

    @Test
    public void testSensitiveScreenAuthDetection() {
        AccessibilityNodeExtractor.ScreenSnapshot snapshot = new AccessibilityNodeExtractor.ScreenSnapshot();
        snapshot.packageName = "com.mventus.selfcare.activity";
        snapshot.isSensitiveAuthScreen = true;
        snapshot.sensitiveReason = "UPI PIN detected";

        ProviderScreenClassifier.ScreenType type = ProviderScreenClassifier.classify(snapshot);
        assertEquals(ProviderScreenClassifier.ScreenType.SENSITIVE_AUTH, type);
    }

    @Test
    public void testScreenContentParserExtractsPlans() {
        AccessibilityNodeExtractor.ScreenSnapshot snapshot = new AccessibilityNodeExtractor.ScreenSnapshot();
        AccessibilityNodeExtractor.ExtractedElement e1 = new AccessibilityNodeExtractor.ExtractedElement();
        e1.text = "Vi Hero Unlimited ₹349";
        snapshot.elements.add(e1);

        AccessibilityNodeExtractor.ExtractedElement e2 = new AccessibilityNodeExtractor.ExtractedElement();
        e2.text = "28 Days • 1.5 GB/day • Truly Unlimited Calls";
        snapshot.elements.add(e2);

        List<RechargePlanEntity> plans = ScreenContentParser.extractPlans(snapshot, "VI");
        assertNotNull(plans);
        assertFalse(plans.isEmpty());
        RechargePlanEntity plan = plans.get(0);
        assertEquals(349.0, plan.price, 0.01);
        assertEquals(28, plan.validityDays);
        assertEquals(1.5, plan.dailyDataGb, 0.01);
    }

    @Test
    public void testScreenContentParserExtractsOffers() {
        AccessibilityNodeExtractor.ScreenSnapshot snapshot = new AccessibilityNodeExtractor.ScreenSnapshot();
        AccessibilityNodeExtractor.ExtractedElement e1 = new AccessibilityNodeExtractor.ExtractedElement();
        e1.text = "Pay with Paytm UPI and get ₹125 cashback on min transaction ₹179";
        snapshot.elements.add(e1);

        List<OfferEntity> offers = ScreenContentParser.extractOffers(snapshot, "VI");
        assertNotNull(offers);
        assertFalse(offers.isEmpty());
        OfferEntity offer = offers.get(0);
        assertEquals("Paytm", offer.provider);
        assertEquals(125.0, offer.cashbackAmount, 0.01);
    }

    @Test
    public void testSimDetectionStates() {
        SimProfileEntity sim1 = new SimProfileEntity(
                "sim_1", "9847837607", "VI", "Vodafone Idea (Vi)", "KL", "Kerala", "SIM 1", true, true
        );
        assertEquals(SimProfileEntity.STATE_NUMBER_AVAILABLE, sim1.detectionState);

        SimProfileEntity sim2 = new SimProfileEntity(
                "sim_2", "", "JIO", "Reliance Jio", "DL", "Delhi NCR", "SIM 2", true, false
        );
        assertEquals(SimProfileEntity.STATE_NUMBER_UNAVAILABLE, sim2.detectionState);

        SimProfileEntity confirmedSim = new SimProfileEntity(
                "sim_user", "9847837666", "VI", "Vodafone Idea (Vi)", "KL", "Kerala", "SIM 1", true, true,
                1, "Vi", "404", "in", "READY", SimProfileEntity.STATE_USER_CONFIRMED, true, true
        );
        assertEquals(SimProfileEntity.STATE_USER_CONFIRMED, confirmedSim.detectionState);
        assertTrue(confirmedSim.isUserConfirmed);
        assertTrue(confirmedSim.isManuallyEntered);
    }
}
