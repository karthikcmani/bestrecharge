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
    public void testScreenContentParserExtractsCoupons() {
        AccessibilityNodeExtractor.ScreenSnapshot snapshot = new AccessibilityNodeExtractor.ScreenSnapshot();
        AccessibilityNodeExtractor.ExtractedElement e1 = new AccessibilityNodeExtractor.ExtractedElement();
        e1.text = "Special Offer: Use code VIRECH20 to get ₹20 discount on recharge";
        snapshot.elements.add(e1);

        List<OfferEntity> offers = ScreenContentParser.extractOffers(snapshot, "VI");
        assertNotNull(offers);
        assertFalse(offers.isEmpty());

        boolean foundCoupon = false;
        for (OfferEntity o : offers) {
            if ("COUPON_CODE".equals(o.category) && o.title.contains("VIRECH20")) {
                foundCoupon = true;
                assertEquals(20.0, o.discountAmount, 0.01);
                assertEquals("VIRECH20", o.paymentMethodRequired);
                break;
            }
        }
        assertTrue("Coupon VIRECH20 should be parsed as COUPON_CODE", foundCoupon);
    }

    @Test
    public void testScreenContentParserExtractsMultipleCardsIndependently() {
        AccessibilityNodeExtractor.ScreenSnapshot snapshot = new AccessibilityNodeExtractor.ScreenSnapshot();

        // Card 1: BHIM UPI at top (y: 100 to 250)
        AccessibilityNodeExtractor.ExtractedElement c1e1 = new AccessibilityNodeExtractor.ExtractedElement();
        c1e1.text = "BHIM Payments App";
        c1e1.top = 100;
        snapshot.elements.add(c1e1);

        AccessibilityNodeExtractor.ExtractedElement c1e2 = new AccessibilityNodeExtractor.ExtractedElement();
        c1e2.text = "Flat ₹30 cashback on min transaction value of ₹99";
        c1e2.top = 170;
        snapshot.elements.add(c1e2);

        // Card 2: Paytm UPI below (y: 400 to 550)
        AccessibilityNodeExtractor.ExtractedElement c2e1 = new AccessibilityNodeExtractor.ExtractedElement();
        c2e1.text = "Paytm UPI";
        c2e1.top = 400;
        snapshot.elements.add(c2e1);

        AccessibilityNodeExtractor.ExtractedElement c2e2 = new AccessibilityNodeExtractor.ExtractedElement();
        c2e2.text = "₹125 assured cashback on min transaction value of ₹199";
        c2e2.top = 470;
        snapshot.elements.add(c2e2);

        List<OfferEntity> offers = ScreenContentParser.extractOffers(snapshot, "VI");
        assertNotNull(offers);
        assertEquals(2, offers.size());

        OfferEntity bhimOffer = offers.get(0).provider.contains("BHIM") ? offers.get(0) : offers.get(1);
        OfferEntity paytmOffer = offers.get(0).provider.contains("Paytm") ? offers.get(0) : offers.get(1);

        assertEquals("BHIM", bhimOffer.provider);
        assertEquals(30.0, bhimOffer.cashbackAmount, 0.01);
        assertEquals(99.0, bhimOffer.minTransactionAmount, 0.01);

        assertEquals("Paytm", paytmOffer.provider);
        assertEquals(125.0, paytmOffer.cashbackAmount, 0.01);
        assertEquals(199.0, paytmOffer.minTransactionAmount, 0.01);
    }

    @Test
    public void testOfferEntityEqualityAndDeduplication() {
        OfferEntity o1 = new OfferEntity(
                "off_1", "Paytm ₹125", "Paytm", "PAYMENT_OFFER", "Desc", "FLAT_CASHBACK",
                0.0, 125.0, 199.0, 125.0, "ALL", "Paytm UPI", "VI", true, "2026-11-30",
                "Terms", "EXTRACTED", "NONE", true
        );
        OfferEntity o2 = new OfferEntity(
                "off_1", "Paytm Duplicate", "Paytm", "PAYMENT_OFFER", "Desc", "FLAT_CASHBACK",
                0.0, 125.0, 199.0, 125.0, "ALL", "Paytm UPI", "VI", true, "2026-11-30",
                "Terms", "EXTRACTED", "NONE", true
        );
        assertEquals(o1, o2);
        assertEquals(o1.hashCode(), o2.hashCode());
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
