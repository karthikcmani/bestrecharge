package com.bestrecharge.app.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.bestrecharge.app.data.local.entities.OfferEntity;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class DealCalculatorTest {

    @Test
    public void testVi349HeroWithPaytm125Cashback() {
        // Vi ₹349 MRP
        // Vi app instant discount: ₹15 off
        // Payable: ₹334
        // Paytm assured cashback: ₹125
        // Expected guaranteed effective cost: ₹209

        OfferEntity opOffer = new OfferEntity(
                "vi_15", "Vi App ₹15 Instant Discount", "Vi", "OPERATOR",
                "Instant discount", "FLAT_DISCOUNT", 15.0, 0.0, 299.0, 15.0,
                "ALL", "ANY", "VI", true, null, "Terms", "OFFICIALLY_VERIFIED", "NONE", true
        );

        OfferEntity paytmOffer = new OfferEntity(
                "paytm_125", "Paytm ₹125 Assured Cashback", "Paytm", "UPI",
                "Cashback", "FLAT_CASHBACK", 0.0, 125.0, 179.0, 125.0,
                "INACTIVE_60_DAYS", "Paytm UPI", "ALL", true, null, "Terms", "OFFICIALLY_VERIFIED", "NONE", true
        );

        UserPreferences prefs = new UserPreferences();
        prefs.isPaytmNewOrInactive60d = true;

        DealBreakdown deal = DealCalculator.evaluateDeal(349.0, opOffer, paytmOffer, null, prefs);

        assertNotNull(deal);
        assertEquals(349.0, deal.originalMrp, 0.01);
        assertEquals(15.0, deal.operatorDiscount, 0.01);
        assertEquals(334.0, deal.immediatePayable, 0.01);
        assertEquals(125.0, deal.guaranteedCashback, 0.01);
        assertEquals(209.0, deal.guaranteedEffectiveCost, 0.01);
        assertEquals(140.0, deal.totalGuaranteedSavings, 0.01);
    }

    @Test
    public void testMinTransactionThresholdRejection() {
        // Plan MRP ₹150, but offer requires min transaction ₹179
        OfferEntity paytmOffer = new OfferEntity(
                "paytm_125", "Paytm ₹125 Cashback", "Paytm", "UPI",
                "Cashback", "FLAT_CASHBACK", 0.0, 125.0, 179.0, 125.0,
                "ALL", "Paytm UPI", "ALL", true, null, "Terms", "OFFICIALLY_VERIFIED", "NONE", true
        );

        DealBreakdown deal = DealCalculator.evaluateDeal(150.0, null, paytmOffer, null, null);
        assertNull(deal);
    }

    @Test
    public void testConditionalCashbackScratchCard() {
        // BHIM up to ₹75: guaranteed cashback is 0, potential is 75
        OfferEntity bhimOffer = new OfferEntity(
                "bhim_75", "BHIM Up to ₹75 Cashback", "BHIM", "UPI",
                "Cashback", "UPTO_CASHBACK", 0.0, 75.0, 99.0, 75.0,
                "ALL", "BHIM Payments App", "ALL", true, null, "Terms", "OFFICIALLY_VERIFIED", "NONE", true
        );

        DealBreakdown deal = DealCalculator.evaluateDeal(349.0, null, bhimOffer, null, null);

        assertNotNull(deal);
        assertEquals(0.0, deal.guaranteedCashback, 0.01);
        assertEquals(75.0, deal.potentialCashback, 0.01);
        assertEquals(349.0, deal.guaranteedEffectiveCost, 0.01);
        assertEquals(274.0, deal.potentialEffectiveCost, 0.01);
    }

    @Test
    public void testFindBestDealsRanking() {
        OfferEntity opOffer = new OfferEntity(
                "vi_15", "Vi ₹15 Off", "Vi", "OPERATOR",
                "Discount", "FLAT_DISCOUNT", 15.0, 0.0, 299.0, 15.0,
                "ALL", "ANY", "VI", true, null, "", "VERIFIED", "NONE", true
        );
        OfferEntity paytmOffer = new OfferEntity(
                "paytm_125", "Paytm ₹125", "Paytm", "UPI",
                "Cashback", "FLAT_CASHBACK", 0.0, 125.0, 179.0, 125.0,
                "ALL", "Paytm UPI", "ALL", true, null, "", "VERIFIED", "NONE", true
        );
        OfferEntity bhimOffer = new OfferEntity(
                "bhim_75", "BHIM Up to ₹75", "BHIM", "UPI",
                "Cashback", "UPTO_CASHBACK", 0.0, 75.0, 99.0, 75.0,
                "ALL", "BHIM", "ALL", true, null, "", "VERIFIED", "NONE", true
        );

        List<OfferEntity> list = new ArrayList<>();
        list.add(opOffer);
        list.add(paytmOffer);
        list.add(bhimOffer);

        List<DealBreakdown> ranked = DealCalculator.findBestDeals(349.0, "VI", list, new UserPreferences());

        assertNotNull(ranked);
        assertEquals("Paytm", ranked.get(0).recommendedPaymentProvider);
        assertEquals(209.0, ranked.get(0).guaranteedEffectiveCost, 0.01);
    }
}
