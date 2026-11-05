package com.bestrecharge.app;

import android.app.Application;

import com.bestrecharge.app.data.local.AppDatabase;
import com.bestrecharge.app.data.local.entities.OfferEntity;
import com.bestrecharge.app.data.local.entities.RechargePlanEntity;
import com.bestrecharge.app.data.local.entities.SavingsRecordEntity;
import com.bestrecharge.app.data.local.entities.SimProfileEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class BestRechargeApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        initDefaultLocalData();
    }

    private void initDefaultLocalData() {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            if (db.simProfileDao().getAllSimProfiles().isEmpty()) {
                // 1. Seed Primary SIM (matching screenshot: 98478 37607, Vi, Delhi)
                SimProfileEntity sim1 = new SimProfileEntity(
                        "sim_default_1",
                        "98478 37607",
                        "VI",
                        "Vodafone Idea (Vi)",
                        "DL",
                        "Delhi NCR",
                        "SIM 1",
                        true,
                        true
                );
                SimProfileEntity sim2 = new SimProfileEntity(
                        "sim_default_2",
                        "98101 23456",
                        "JIO",
                        "Reliance Jio",
                        "DL",
                        "Delhi NCR",
                        "SIM 2",
                        true,
                        false
                );
                List<SimProfileEntity> sims = new ArrayList<>();
                sims.add(sim1);
                sims.add(sim2);
                db.simProfileDao().insertAll(sims);

                // 2. Seed Plans
                List<RechargePlanEntity> plans = new ArrayList<>();
                // Vi Plans
                plans.add(new RechargePlanEntity(
                        "vi_349_hero", "VI", "Vodafone Idea (Vi)", "DL",
                        349.0, 28, 1.5, 42.0, "Truly Unlimited Calls", 100,
                        "Vi Movies & TV, Weekend Rollover, Binge All Night (12am-6am)",
                        "UNLIMITED", "Hero Unlimited: Unlimited Night Data 12am-6am, Weekend Rollover, 1.5GB/day for 28 days.",
                        "Official Vi Prepaid Tariff", "2026-11-06", true
                ));
                plans.add(new RechargePlanEntity(
                        "vi_479_hero", "VI", "Vodafone Idea (Vi)", "DL",
                        479.0, 48, 1.5, 72.0, "Truly Unlimited Calls", 100,
                        "Weekend Data Rollover, Binge All Night",
                        "DAILY_DATA", "1.5GB/day with Binge All Night unlimited night internet.",
                        "Official Vi Prepaid Tariff", "2026-11-06", true
                ));
                plans.add(new RechargePlanEntity(
                        "vi_859_hero", "VI", "Vodafone Idea (Vi)", "DL",
                        859.0, 84, 1.5, 126.0, "Truly Unlimited Calls", 100,
                        "Weekend Data Rollover, Binge All Night",
                        "LONG_VALIDITY", "84 Days 1.5GB/day Hero Unlimited package.",
                        "Official Vi Prepaid Tariff", "2026-11-06", true
                ));
                plans.add(new RechargePlanEntity(
                        "vi_199_data", "VI", "Vodafone Idea (Vi)", "DL",
                        199.0, 18, 1.0, 18.0, "Truly Unlimited Calls", 100,
                        "", "POPULAR", "Affordable short validity unlimited calling and daily 1GB.",
                        "Official Vi Prepaid Tariff", "2026-11-06", true
                ));

                // Jio Plans
                plans.add(new RechargePlanEntity(
                        "jio_349_popular", "JIO", "Reliance Jio", "DL",
                        349.0, 28, 2.0, 56.0, "Truly Unlimited Calls", 100,
                        "Unlimited True 5G, JioCinema, JioTV",
                        "UNLIMITED", "True 5G Unlimited, 2GB/day for 28 days with Jio apps.",
                        "Official Jio Portal", "2026-11-06", true
                ));
                plans.add(new RechargePlanEntity(
                        "jio_899_quarterly", "JIO", "Reliance Jio", "DL",
                        899.0, 90, 2.0, 180.0, "Truly Unlimited Calls", 100,
                        "Unlimited True 5G, JioTV, JioCinema",
                        "LONG_VALIDITY", "90 Days uninterrupted 2GB/day + 20GB extra data.",
                        "Official Jio Portal", "2026-11-06", true
                ));

                // Airtel Plans
                plans.add(new RechargePlanEntity(
                        "airtel_349_popular", "AIRTEL", "Bharti Airtel", "DL",
                        349.0, 28, 1.5, 42.0, "Truly Unlimited Calls", 100,
                        "Unlimited 5G Data, Airtel Xstream Play, Wynk Music",
                        "UNLIMITED", "Unlimited 5G, 1.5GB/day 28 days with Wynk and Apollo 24/7.",
                        "Official Airtel Tariff", "2026-11-06", true
                ));

                // BSNL Plans
                plans.add(new RechargePlanEntity(
                        "bsnl_199_value", "BSNL", "BSNL", "DL",
                        199.0, 30, 2.0, 60.0, "Unlimited Local & STD Calls", 100,
                        "", "POPULAR", "Affordable high-data 2GB/day for 30 full days.",
                        "Official BSNL Tariff", "2026-11-06", true
                ));
                plans.add(new RechargePlanEntity(
                        "bsnl_397_long", "BSNL", "BSNL", "DL",
                        397.0, 150, 2.0, 60.0, "Unlimited for 30 days, incoming valid 150 days", 100,
                        "", "LONG_VALIDITY", "Long validity SIM keeping pack: 150 days validity with 2GB/day for first 30 days.",
                        "Official BSNL Tariff", "2026-11-06", true
                ));
                db.rechargePlanDao().insertAll(plans);

                // 3. Seed Offers (matching uploaded screenshot terms!)
                List<OfferEntity> offers = new ArrayList<>();
                // Vi App Exclusive ₹15 off
                offers.add(new OfferEntity(
                        "vi_app_15_off", "Vi App Exclusive ₹15 Instant Discount",
                        "Vi", "OPERATOR",
                        "Direct instant deduction of ₹15 applied when recharging on Vi App.",
                        "FLAT_DISCOUNT", 15.0, 0.0, 299.0, 15.0,
                        "ALL", "ANY", "VI", true,
                        "2026-11-30", "Instant ₹15 discount applied at checkout on Vi app for recharge >= ₹299.",
                        "OFFICIALLY_VERIFIED", "NONE", true
                ));

                // Paytm UPI ₹125 assured cashback
                offers.add(new OfferEntity(
                        "paytm_upi_125_cb", "Paytm UPI Assured ₹125 Cashback",
                        "Paytm", "UPI",
                        "Get assured ₹125 cashback for users who have not used Paytm anywhere in the last 60 days.",
                        "FLAT_CASHBACK", 0.0, 125.0, 179.0, 125.0,
                        "INACTIVE_60_DAYS", "Paytm UPI", "ALL", true,
                        "2026-11-30", "Min assured cashback of ₹125. Applicable on min transaction value of ₹179 using Paytm UPI. Offer valid for users with no transaction in last 60 days.",
                        "OFFICIALLY_VERIFIED", "NONE", true
                ));

                // BHIM Payments App up to ₹75 cashback
                offers.add(new OfferEntity(
                        "bhim_upi_75_cb", "BHIM Payments App Up to ₹75 Cashback",
                        "BHIM", "UPI",
                        "Get assured cashback up to ₹75 on min transaction of ₹99 via BHIM payments app.",
                        "UPTO_CASHBACK", 0.0, 75.0, 99.0, 75.0,
                        "ALL", "BHIM Payments App", "ALL", true,
                        "2026-11-30", "Offer valid from 1st to 30th November 2026. Applicable on payments using BHIM Payments App on min transaction value of ₹99.",
                        "OFFICIALLY_VERIFIED", "NONE", true
                ));

                // PhonePe ₹25 cashback
                offers.add(new OfferEntity(
                        "phonepe_flat_25", "PhonePe Assured ₹25 Cashback",
                        "PhonePe", "UPI",
                        "Flat ₹25 cashback on mobile recharge of ₹199 or more using PhonePe UPI.",
                        "FLAT_CASHBACK", 0.0, 25.0, 199.0, 25.0,
                        "ALL", "PhonePe UPI", "ALL", true,
                        "2026-11-30", "Cashback credited directly to bank account within 24 hours.",
                        "PUBLICLY_SOURCED", "NONE", true
                ));

                // Vi Stars Reward Points
                offers.add(new OfferEntity(
                        "vi_stars_25", "Earn 25 Vi Stars Loyalty Points",
                        "Vi", "REWARD",
                        "Earn 25 Vi Stars points redeemable for partner coupons.",
                        "REWARD_POINTS", 0.0, 25.0, 199.0, 25.0,
                        "ALL", "ANY", "VI", true,
                        "2026-11-30", "Vi Stars loyalty points credited to user account. Non-fiat rewards.",
                        "OFFICIALLY_VERIFIED", "NONE", true
                ));
                db.offerDao().insertAll(offers);

                // 4. Seed Savings Record History
                List<SavingsRecordEntity> records = new ArrayList<>();
                records.add(new SavingsRecordEntity(
                        "rec_oct_01", "98478 37607", "Vi", "Delhi NCR",
                        "Vi Hero Unlimited ₹349", 349.0, 15.0, 334.0,
                        "Paytm UPI", "Paytm UPI ₹125 Assured Cashback",
                        125.0, 125.0, "RECEIVED", 209.0,
                        "2026-10-05 14:30", "Recharge successful. Received ₹125 cashback."
                ));
                records.add(new SavingsRecordEntity(
                        "rec_sep_01", "98478 37607", "Vi", "Delhi NCR",
                        "Vi Hero Unlimited ₹349", 349.0, 15.0, 334.0,
                        "BHIM Payments App", "BHIM Up to ₹75 Cashback",
                        75.0, 45.0, "RECEIVED", 289.0,
                        "2026-09-07 11:15", "Received ₹45 cashback via BHIM."
                ));
                db.savingsRecordDao().insertAll(records);
            }
        });
    }
}
