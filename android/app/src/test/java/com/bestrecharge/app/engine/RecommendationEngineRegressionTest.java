package com.bestrecharge.app.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.bestrecharge.app.data.local.entities.OfferEntity;
import com.bestrecharge.app.data.local.entities.RechargePlanEntity;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class RecommendationEngineRegressionTest {

    private List<RechargePlanEntity> samplePlans;
    private List<OfferEntity> sampleOffers;
    private UserPreferences userPrefs;

    @Before
    public void setUp() {
        samplePlans = new ArrayList<>();
        // Plan A: ₹199, 18 days, 1 GB/day
        samplePlans.add(new RechargePlanEntity(
                "plan_199", "VI", "Vi", "DL", 199.0, 18, 1.0, 18.0,
                "Unlimited", 100, "", "DAILY_DATA", "1GB/day short pack", "Tariff", "2026-11-06", true
        ));
        // Plan B: ₹179, 28 days, 0 daily data (2GB total) -> VALIDITY PACK
        samplePlans.add(new RechargePlanEntity(
                "plan_179_val", "VI", "Vi", "DL", 179.0, 28, 0.0, 2.0,
                "Unlimited", 300, "", "VALIDITY_ONLY", "28 days validity pack", "Tariff", "2026-11-06", true
        ));
        // Plan C: ₹349, 28 days, 1.5 GB/day -> HERO PACK
        samplePlans.add(new RechargePlanEntity(
                "plan_349_hero", "VI", "Vi", "DL", 349.0, 28, 1.5, 42.0,
                "Unlimited", 100, "Night Binge", "UNLIMITED", "1.5GB/day hero pack", "Tariff", "2026-11-06", true
        ));
        // Plan D: ₹399, 28 days, 2.5 GB/day -> HEAVY DATA
        samplePlans.add(new RechargePlanEntity(
                "plan_399_heavy", "VI", "Vi", "DL", 399.0, 28, 2.5, 70.0,
                "Unlimited", 100, "Hotstar", "DAILY_DATA", "2.5GB/day data pack", "Tariff", "2026-11-06", true
        ));
        // Plan E: ₹859, 84 days, 1.5 GB/day -> QUARTERLY PACK
        samplePlans.add(new RechargePlanEntity(
                "plan_859_quarterly", "VI", "Vi", "DL", 859.0, 84, 1.5, 126.0,
                "Unlimited", 100, "", "LONG_VALIDITY", "84 days quarterly pack", "Tariff", "2026-11-06", true
        ));
        // Plan F: ₹1999, 365 days, 0 daily data (24GB total) -> ANNUAL VALIDITY PACK
        samplePlans.add(new RechargePlanEntity(
                "plan_1999_annual", "VI", "Vi", "DL", 1999.0, 365, 0.0, 24.0,
                "Unlimited", 3600, "", "VALIDITY_ONLY", "365 days annual validity", "Tariff", "2026-11-06", true
        ));

        sampleOffers = new ArrayList<>();
        // Vi App ₹15 discount for >= ₹299
        sampleOffers.add(new OfferEntity(
                "vi_15", "Vi App ₹15 Off", "Vi", "OPERATOR",
                "Instant discount", "FLAT_DISCOUNT", 15.0, 0.0, 299.0, 15.0,
                "ALL", "ANY", "VI", true, "2026-11-30", "Terms", "VERIFIED", "NONE", true
        ));
        // Paytm ₹125 cashback for >= ₹179
        sampleOffers.add(new OfferEntity(
                "paytm_125", "Paytm ₹125 Assured", "Paytm", "UPI",
                "Cashback", "FLAT_CASHBACK", 0.0, 125.0, 179.0, 125.0,
                "INACTIVE_60_DAYS", "Paytm UPI", "ALL", true, "2026-11-30", "Terms", "VERIFIED", "NONE", true
        ));
        // BHIM up to ₹75 scratch card
        sampleOffers.add(new OfferEntity(
                "bhim_75", "BHIM up to ₹75", "BHIM", "UPI",
                "Scratch card", "UPTO_CASHBACK", 0.0, 75.0, 99.0, 75.0,
                "ALL", "BHIM", "ALL", true, "2026-11-30", "Terms", "VERIFIED", "NONE", true
        ));

        userPrefs = new UserPreferences();
        userPrefs.isPaytmNewOrInactive60d = true;
        userPrefs.isBhimNewUser = true;
    }

    /**
     * TEST 1: Select Validity Only
     * Expected: Plans are ranked according to validity, not retaining the static ₹349 MRP plan.
     */
    @Test
    public void test01_selectValidityOnly_ranksByValidityNotStaticMrp() {
        RechargePreference pref = new RechargePreference(
                RechargePreference.DataAllowance.VALIDITY_ONLY, 0, 0,
                RechargePreference.RankingPriority.MAXIMUM_VALIDITY, "VI"
        );

        RecommendationEngine.RecommendationResult result = RecommendationEngine.evaluateAndRank(
                samplePlans, sampleOffers, userPrefs, pref
        );

        assertNotNull(result.topDeal);
        assertNotEquals("plan_349_hero", result.topDeal.rechargePlan.id);
        // Longest validity in dataset is 365 days (Plan F)
        assertEquals(365, result.topDeal.validityDays);
        assertEquals("plan_1999_annual", result.topDeal.rechargePlan.id);
    }

    /**
     * TEST 2: Select 1 GB/day
     * Expected: Plans satisfying the daily data requirement are prioritised.
     */
    @Test
    public void test02_select1GbPerDay_prioritizesQualifyingPlans() {
        RechargePreference pref = new RechargePreference(
                RechargePreference.DataAllowance.GB_1_DAY, 0, 0,
                RechargePreference.RankingPriority.BEST_OVERALL_VALUE, "VI"
        );

        RecommendationEngine.RecommendationResult result = RecommendationEngine.evaluateAndRank(
                samplePlans, sampleOffers, userPrefs, pref
        );

        assertNotNull(result.topDeal);
        assertTrue(result.topDeal.rechargePlan.dailyDataGb >= 1.0);
    }

    /**
     * TEST 3: Select 2 GB/day
     * Expected: 1 GB/day and 1.5 GB/day plans are excluded when 2 GB/day is a mandatory preference.
     */
    @Test
    public void test03_select2GbPerDay_excludesLowerDataPlans() {
        RechargePreference pref = new RechargePreference(
                RechargePreference.DataAllowance.GB_2_DAY, 0, 0,
                RechargePreference.RankingPriority.BEST_OVERALL_VALUE, "VI"
        );

        RecommendationEngine.RecommendationResult result = RecommendationEngine.evaluateAndRank(
                samplePlans, sampleOffers, userPrefs, pref
        );

        assertNotNull(result.topDeal);
        assertEquals("plan_399_heavy", result.topDeal.rechargePlan.id);
        assertTrue(result.topDeal.rechargePlan.dailyDataGb >= 2.0);

        for (DealBreakdown deal : result.allRankedDeals) {
            assertTrue(deal.rechargePlan.dailyDataGb >= 2.0);
        }
    }

    /**
     * TEST 4: Select maximum validity
     * Expected: The longest eligible plans rank first.
     */
    @Test
    public void test04_selectMaximumValidity_longestPlansRankFirst() {
        RechargePreference pref = new RechargePreference(
                RechargePreference.DataAllowance.ANY, 0, 0,
                RechargePreference.RankingPriority.MAXIMUM_VALIDITY, "VI"
        );

        RecommendationEngine.RecommendationResult result = RecommendationEngine.evaluateAndRank(
                samplePlans, sampleOffers, userPrefs, pref
        );

        assertNotNull(result.topDeal);
        assertEquals(365, result.topDeal.validityDays);

        int prevValidity = Integer.MAX_VALUE;
        for (DealBreakdown deal : result.allRankedDeals) {
            assertTrue(deal.validityDays <= prevValidity);
            prevValidity = deal.validityDays;
        }
    }

    /**
     * TEST 5: Set maximum budget ₹300
     * Expected: Plans above ₹300 are excluded when budget is a hard limit.
     */
    @Test
    public void test05_setMaxBudget300_excludesPlansAbove300() {
        RechargePreference pref = new RechargePreference(
                RechargePreference.DataAllowance.ANY, 0, 300.0,
                RechargePreference.RankingPriority.LOWEST_EFFECTIVE_COST, "VI"
        );

        RecommendationEngine.RecommendationResult result = RecommendationEngine.evaluateAndRank(
                samplePlans, sampleOffers, userPrefs, pref
        );

        assertNotNull(result.topDeal);
        assertTrue(result.topDeal.rechargePlan.price <= 300.0);

        for (DealBreakdown deal : result.allRankedDeals) {
            assertTrue("Plan price must not exceed budget ₹300: " + deal.rechargePlan.price,
                    deal.rechargePlan.price <= 300.0);
        }
    }

    /**
     * TEST 6: Change from Validity Only to Lowest Effective Cost
     * Expected: The recommendation recalculates.
     */
    @Test
    public void test06_changeFromValidityOnlyToLowestCost_recalculates() {
        RechargePreference valPref = new RechargePreference(
                RechargePreference.DataAllowance.VALIDITY_ONLY, 0, 0,
                RechargePreference.RankingPriority.MAXIMUM_VALIDITY, "VI"
        );
        RecommendationEngine.RecommendationResult valResult = RecommendationEngine.evaluateAndRank(
                samplePlans, sampleOffers, userPrefs, valPref
        );

        RechargePreference costPref = new RechargePreference(
                RechargePreference.DataAllowance.ANY, 0, 0,
                RechargePreference.RankingPriority.LOWEST_EFFECTIVE_COST, "VI"
        );
        RecommendationEngine.RecommendationResult costResult = RecommendationEngine.evaluateAndRank(
                samplePlans, sampleOffers, userPrefs, costPref
        );

        assertNotEquals(valResult.topDeal.rechargePlan.id, costResult.topDeal.rechargePlan.id);
        assertTrue(costResult.topDeal.potentialEffectiveCost <= valResult.topDeal.potentialEffectiveCost);
    }

    /**
     * TEST 7: Select two compatible offers
     * Expected: Both discounts are calculated correctly.
     */
    @Test
    public void test07_selectTwoCompatibleOffers_stacksCorrectly() {
        // ₹349 with Vi ₹15 off and Paytm ₹125 assured cashback
        DealBreakdown deal = DealCalculator.findBestDeals(349.0, "VI", sampleOffers, userPrefs).get(0);

        assertEquals(349.0, deal.originalMrp, 0.01);
        assertEquals(15.0, deal.operatorDiscount, 0.01);
        assertEquals(334.0, deal.immediatePayable, 0.01);
        assertEquals(125.0, deal.guaranteedCashback, 0.01);
        assertEquals(209.0, deal.potentialEffectiveCost, 0.01);
    }

    /**
     * TEST 8: Select mutually exclusive offers
     * Expected: The engine does not combine them.
     */
    @Test
    public void test08_mutuallyExclusiveOffers_doesNotCombine() {
        List<OfferEntity> conflictingOffers = new ArrayList<>();
        conflictingOffers.add(new OfferEntity(
                "paytm_promo", "Paytm UPI ₹50", "Paytm", "UPI",
                "", "FLAT_CASHBACK", 0.0, 50.0, 100.0, 50.0,
                "ALL", "Paytm UPI", "ALL", true, null, "", "VERIFIED", "NONE", true
        ));
        conflictingOffers.add(new OfferEntity(
                "bhim_promo", "BHIM UPI ₹40", "BHIM", "UPI",
                "", "FLAT_CASHBACK", 0.0, 40.0, 100.0, 40.0,
                "ALL", "BHIM", "ALL", true, null, "", "VERIFIED", "NONE", true
        ));

        List<DealBreakdown> deals = DealCalculator.findBestDeals(199.0, "VI", conflictingOffers, userPrefs);
        assertNotNull(deals);
        assertFalse(deals.isEmpty());
        // Mutually exclusive payment methods must not be summed together (50 + 40 != 90)
        DealBreakdown top = deals.get(0);
        assertTrue(top.guaranteedCashback <= 50.0);
    }

    /**
     * TEST 9: Conditional cashback
     * Expected: It is displayed separately from guaranteed savings.
     */
    @Test
    public void test09_conditionalCashback_displayedSeparatelyFromGuaranteed() {
        List<OfferEntity> scratchCardOffers = new ArrayList<>();
        scratchCardOffers.add(new OfferEntity(
                "scratch_75", "Up to ₹75 Scratch Card", "BHIM", "UPI",
                "", "UPTO_CASHBACK", 0.0, 75.0, 99.0, 75.0,
                "ALL", "BHIM", "ALL", true, null, "", "VERIFIED", "NONE", true
        ));

        DealBreakdown deal = DealCalculator.findBestDeals(199.0, "VI", scratchCardOffers, userPrefs).get(0);
        assertEquals(0.0, deal.guaranteedCashback, 0.01);
        assertEquals(75.0, deal.potentialCashback, 0.01);
        assertEquals(199.0, deal.guaranteedEffectiveCost, 0.01);
        assertEquals(124.0, deal.potentialEffectiveCost, 0.01);
    }

    /**
     * TEST 10: Expired offer
     * Expected: It cannot be recommended as an active offer.
     */
    @Test
    public void test10_expiredOffer_notRecommended() {
        List<OfferEntity> expiredList = new ArrayList<>();
        expiredList.add(new OfferEntity(
                "old_offer", "Expired ₹100 Off", "Vi", "OPERATOR",
                "", "FLAT_DISCOUNT", 100.0, 0.0, 100.0, 100.0,
                "ALL", "ANY", "VI", true, "2020-01-01", "", "VERIFIED", "NONE", true
        ));

        List<DealBreakdown> deals = DealCalculator.findBestDeals(349.0, "VI", expiredList, userPrefs);
        for (DealBreakdown d : deals) {
            assertEquals(0.0, d.operatorDiscount, 0.01);
        }
    }

    /**
     * TEST 11: Unknown eligibility
     * Expected: The engine does not assume the user qualifies.
     */
    @Test
    public void test11_unknownEligibility_doesNotAssumeQualified() {
        UserPreferences unknownUser = new UserPreferences();
        unknownUser.isPaytmNewOrInactive60d = false; // Existing active Paytm user

        List<DealBreakdown> deals = DealCalculator.findBestDeals(349.0, "VI", sampleOffers, unknownUser);
        for (DealBreakdown d : deals) {
            if ("Paytm".equalsIgnoreCase(d.recommendedPaymentProvider)) {
                // Should not receive the ₹125 inactive-user offer
                assertNotEquals(125.0, d.guaranteedCashback, 0.01);
            }
        }
    }

    /**
     * TEST 12: Missing plan data & divide-by-zero protection
     * Expected: The engine handles missing fields without crashing.
     */
    @Test
    public void test12_missingPlanData_handlesGracefullyWithoutDivideByZero() {
        List<RechargePlanEntity> edgePlans = new ArrayList<>();
        // Plan with 0 validity days and 0 daily data
        edgePlans.add(new RechargePlanEntity(
                "edge_0", "VI", "Vi", "DL", 50.0, 0, 0.0, 0.0,
                null, 0, null, null, null, null, null, true
        ));

        RechargePreference pref = new RechargePreference();
        RecommendationEngine.RecommendationResult result = RecommendationEngine.evaluateAndRank(
                edgePlans, sampleOffers, userPrefs, pref
        );

        assertNotNull(result);
        assertNotNull(result.topDeal);
        // Must calculate without ArithmeticException
        assertTrue(result.topDeal.originalCostPerDay >= 0);
        assertTrue(result.topDeal.effectiveCostPerDay >= 0);
    }

    /**
     * TEST 13: DoT Indian National Numbering Plan Series Allocation
     * Expected: 98478 37666 and 98478 37607 must strictly allocate to Vodafone Idea (Vi) Kerala.
     */
    @Test
    public void test13_dotSeriesAllocation_keralaViNumberDetectedCorrectly() {
        com.bestrecharge.app.utils.TelecomNumberSeriesHelper.AllocationInfo alloc1 =
                com.bestrecharge.app.utils.TelecomNumberSeriesHelper.getAllocation("+91 98478 37666");
        assertNotNull(alloc1);
        assertEquals("VI", alloc1.operatorCode);
        assertEquals("Vodafone Idea (Vi)", alloc1.operatorName);
        assertEquals("Kerala", alloc1.circleName);
        assertEquals("KL", alloc1.circleCode);

        com.bestrecharge.app.utils.TelecomNumberSeriesHelper.AllocationInfo alloc2 =
                com.bestrecharge.app.utils.TelecomNumberSeriesHelper.getAllocation("9847837607");
        assertNotNull(alloc2);
        assertEquals("VI", alloc2.operatorCode);
        assertEquals("Kerala", alloc2.circleName);

        // Test Jio series (starts with 700 / 620)
        com.bestrecharge.app.utils.TelecomNumberSeriesHelper.AllocationInfo allocJio =
                com.bestrecharge.app.utils.TelecomNumberSeriesHelper.getAllocation("7001234567");
        assertNotNull(allocJio);
        assertEquals("JIO", allocJio.operatorCode);

        // Test Airtel Delhi series (9810)
        com.bestrecharge.app.utils.TelecomNumberSeriesHelper.AllocationInfo allocAirtel =
                com.bestrecharge.app.utils.TelecomNumberSeriesHelper.getAllocation("9810123456");
        assertNotNull(allocAirtel);
        assertEquals("AIRTEL", allocAirtel.operatorCode);
    }

    /**
     * TEST 14: Multi-Preference Filter Combination (Budget + Daily Data)
     * Expected: User selecting < ₹350 AND 1.5 GB/day must only return Plan C (₹349 Hero 1.5GB/day).
     */
    @Test
    public void test14_multiPreference_budgetAndDataCombined() {
        RechargePreference multiPref = new RechargePreference();
        multiPref.operatorCode = "VI";
        multiPref.maxBudget = 350.0;
        multiPref.dataAllowance = RechargePreference.DataAllowance.GB_1_5_DAY;

        RecommendationEngine.RecommendationResult result = RecommendationEngine.evaluateAndRank(
                samplePlans, sampleOffers, userPrefs, multiPref
        );

        assertNotNull(result);
        assertNotNull(result.topDeal);
        assertEquals("plan_349_hero", result.topDeal.rechargePlan.id);
        assertEquals(349.0, result.topDeal.originalMrp, 0.01);
    }

    /**
     * TEST 15: Multi-Preference Filter Combination (Validity Only + Budget < ₹200)
     * Expected: User selecting Validity Only AND < ₹200 returns Plan B (₹179 Validity pack).
     */
    @Test
    public void test15_multiPreference_validityOnlyAndUnder200() {
        RechargePreference multiPref = new RechargePreference();
        multiPref.operatorCode = "VI";
        multiPref.maxBudget = 200.0;
        multiPref.dataAllowance = RechargePreference.DataAllowance.VALIDITY_ONLY;
        multiPref.priority = RechargePreference.RankingPriority.MAXIMUM_VALIDITY;

        RecommendationEngine.RecommendationResult result = RecommendationEngine.evaluateAndRank(
                samplePlans, sampleOffers, userPrefs, multiPref
        );

        assertNotNull(result);
        assertNotNull(result.topDeal);
        assertEquals("plan_179_val", result.topDeal.rechargePlan.id);
        assertEquals(179.0, result.topDeal.originalMrp, 0.01);
        assertEquals(28, result.topDeal.rechargePlan.validityDays);
    }
}
