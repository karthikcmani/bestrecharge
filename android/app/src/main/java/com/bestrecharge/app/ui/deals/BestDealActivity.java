package com.bestrecharge.app.ui.deals;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bestrecharge.app.data.local.AppDatabase;
import com.bestrecharge.app.data.local.entities.OfferEntity;
import com.bestrecharge.app.data.local.entities.RechargePlanEntity;
import com.bestrecharge.app.data.local.entities.SavingsRecordEntity;
import com.bestrecharge.app.data.local.entities.SimProfileEntity;
import com.bestrecharge.app.data.repository.SavingsRepository;
import com.bestrecharge.app.databinding.ActivityBestDealBinding;
import com.bestrecharge.app.engine.DealBreakdown;
import com.bestrecharge.app.engine.DealCalculator;
import com.bestrecharge.app.engine.UserPreferences;
import com.bestrecharge.app.utils.TelecomRedirectionHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Executors;

public class BestDealActivity extends AppCompatActivity {

    private ActivityBestDealBinding binding;
    private DealBreakdown activeDeal;
    private RechargePlanEntity activePlan;
    private SimProfileEntity primarySim;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBestDealBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(insets.left, insets.top, insets.right, 0);
            return windowInsets;
        });

        binding.btnDealBack.setOnClickListener(v -> finish());

        String planId = getIntent().getStringExtra("PLAN_ID");
        if (planId == null) {
            planId = "vi_349_hero";
        }

        loadDealData(planId);

        binding.btnContinueToRecharge.setOnClickListener(v -> {
            if (activeDeal != null && activePlan != null) {
                recordAndOpenPayment(activeDeal);
            }
        });
    }

    private void loadDealData(String planId) {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            RechargePlanEntity plan = db.rechargePlanDao().getPlanById(planId);
            if (plan == null) {
                plan = db.rechargePlanDao().getPlanById("vi_349_hero");
            }
            activePlan = plan;

            primarySim = db.simProfileDao().getPrimarySimProfile();
            List<OfferEntity> offers = db.offerDao().getAllOffers();

            UserPreferences prefs = new UserPreferences();
            prefs.isPaytmNewOrInactive60d = true;
            prefs.isBhimNewUser = true;

            List<DealBreakdown> deals = DealCalculator.findBestDeals(
                    plan.price, plan.operatorCode, offers, prefs
            );

            final RechargePlanEntity finalPlan = plan;
            if (!deals.isEmpty()) {
                activeDeal = deals.get(0);
                runOnUiThread(() -> bindDeal(activeDeal, finalPlan));
            }
        });
    }

    private void bindDeal(DealBreakdown deal, RechargePlanEntity plan) {
        deal.validityDays = plan.validityDays;
        deal.recalculateDailyCosts();
        binding.tvDetailPlanHeader.setText(plan.operatorName + " – ₹" + (int) plan.price);
        binding.tvDetailPlanSpecs.setText(plan.validityDays + " Days • " +
                (plan.dailyDataGb > 0 ? plan.dailyDataGb + "GB/day" : plan.totalDataGb + "GB Total") +
                " • " + plan.voiceBenefit);

        binding.tvDetailSavingsBadge.setText("Save ₹" + (int) deal.totalGuaranteedSavings);
        binding.tvRowMrp.setText("₹" + (int) deal.originalMrp);
        binding.tvRowOpDiscount.setText("- ₹" + (int) deal.operatorDiscount);
        binding.tvRowPayable.setText("₹" + (int) deal.immediatePayable);
        binding.tvRowPaymentMethod.setText(deal.recommendedPaymentProvider);
        binding.tvRowCashback.setText("+ ₹" + (int) (deal.guaranteedCashback > 0 ? deal.guaranteedCashback : deal.potentialCashback));
        binding.tvRowEffective.setText("₹" + (int) deal.potentialEffectiveCost);

        binding.tvDetailDailyCost.setText(deal.getDailyCostComparisonString());
        binding.tvDetailExplanation.setText(deal.explanation);
        binding.btnContinueToRecharge.setText(deal.actionLabel != null ? deal.actionLabel : "Continue to Recharge");

        binding.btnDetailVerifyOfficial.setOnClickListener(v -> {
            if (activePlan != null) {
                String mobile = primarySim != null && primarySim.mobileNumber != null ? primarySim.mobileNumber : "9847837607";
                TelecomRedirectionHelper.redirectToRechargeCheckout(
                        this,
                        activePlan.operatorCode,
                        mobile,
                        activePlan.price,
                        "Verify official plan ₹" + (int) activePlan.price
                );
            }
        });

        binding.cardAltBhim.setOnClickListener(v -> {
            Toast.makeText(this, "Selected BHIM alternative deal", Toast.LENGTH_SHORT).show();
        });
        binding.cardAltPhonepe.setOnClickListener(v -> {
            Toast.makeText(this, "Selected PhonePe alternative deal", Toast.LENGTH_SHORT).show();
        });
    }

    private void openOfficialTelecomApp(String opCode) {
        String mobile = primarySim != null && primarySim.mobileNumber != null ? primarySim.mobileNumber : "9847837607";
        double price = activePlan != null ? activePlan.price : 349.0;
        TelecomRedirectionHelper.redirectToRechargeCheckout(this, opCode, mobile, price, null);
    }

    private void recordAndOpenPayment(DealBreakdown deal) {
        String mobile = primarySim != null && primarySim.mobileNumber != null ? primarySim.mobileNumber : "9847837607";
        String operator = activePlan != null ? activePlan.operatorName : "Vi";
        String opCode = activePlan != null && activePlan.operatorCode != null ? activePlan.operatorCode : "VI";
        String circle = primarySim != null && primarySim.circleName != null ? primarySim.circleName : "Delhi NCR";
        String planName = activePlan != null ? activePlan.description : "Mobile Recharge";
        String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date());

        String opAppName = "JIO".equalsIgnoreCase(opCode) ? "MyJio App" :
                "AIRTEL".equalsIgnoreCase(opCode) ? "Airtel Thanks" :
                "BSNL".equalsIgnoreCase(opCode) ? "BSNL Portal" : "Vi App";

        // Show clear, actionable provider handover instructions
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Recharge via " + opAppName)
                .setMessage("Number: +91 " + mobile + "\n" +
                        "Plan: ₹" + (int) deal.originalMrp + "\n\n" +
                        "Instructions:\n" +
                        "1. Opening " + opAppName + " recharge flow.\n" +
                        (deal.operatorDiscount > 0 ? "2. Apply ₹" + (int) deal.operatorDiscount + " app-exclusive discount.\n" : "") +
                        "3. Select " + deal.recommendedPaymentProvider + " on the payment screen to get your ₹" +
                        (int) (deal.guaranteedCashback > 0 ? deal.guaranteedCashback : deal.potentialCashback) + " cashback credited to your account!")
                .setPositiveButton("Open " + opAppName, (dialog, which) -> {
                    // 1. Log transaction in local Room savings history
                    SavingsRecordEntity record = new SavingsRecordEntity(
                            "rec_" + UUID.randomUUID().toString().substring(0, 8),
                            mobile, operator, circle, planName,
                            deal.originalMrp, deal.operatorDiscount, deal.immediatePayable,
                            deal.recommendedPaymentProvider,
                            deal.stackedOffers != null && !deal.stackedOffers.isEmpty() ? deal.stackedOffers.get(0) : "Smart Offer",
                            deal.guaranteedCashback > 0 ? deal.guaranteedCashback : deal.potentialCashback,
                            0.0, "PENDING", deal.potentialEffectiveCost, dateStr,
                            "Initiated via " + opAppName + " with " + deal.recommendedPaymentProvider
                    );
                    new SavingsRepository(getApplication()).insertRecord(record);
                    com.bestrecharge.app.BestRechargeApp.pendingCelebrationRecord = record;

                    // 2. Launch provider app directly to checkout page
                    String promoSummary = "Recharge ₹" + (int) deal.originalMrp + " for +91 " + mobile +
                            (deal.operatorDiscount > 0 ? " | Save ₹" + (int) deal.operatorDiscount : "") +
                            " | Pay with " + deal.recommendedPaymentProvider + " for ₹" +
                            (int) (deal.guaranteedCashback > 0 ? deal.guaranteedCashback : deal.potentialCashback) + " cashback";
                    TelecomRedirectionHelper.redirectToRechargeCheckout(
                            this,
                            opCode,
                            mobile,
                            deal.originalMrp,
                            promoSummary
                    );
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (com.bestrecharge.app.BestRechargeApp.pendingCelebrationRecord != null) {
            SavingsRecordEntity rec = com.bestrecharge.app.BestRechargeApp.pendingCelebrationRecord;
            com.bestrecharge.app.BestRechargeApp.pendingCelebrationRecord = null;
            showSavingsThankYouDialog(rec);
        }
    }

    private void showSavingsThankYouDialog(SavingsRecordEntity rec) {
        int totalSaved = (int) (rec.originalMrp - rec.effectiveCost);
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("🎉 Recharge Completed!")
                .setMessage("Congratulations! You saved ₹" + totalSaved + " on your recharge for +91 " + rec.mobileNumber + "!\n\n" +
                        "• Original MRP: ₹" + (int) rec.originalMrp + "\n" +
                        "• Amount Paid: ₹" + (int) rec.amountPaid + "\n" +
                        "• Final Effective Cost: ₹" + (int) rec.effectiveCost + "\n" +
                        "• Assured Savings: ₹" + totalSaved + "\n\n" +
                        "Thank you for recharging smartly with BestRecharge! Your transaction is logged in your Savings Tracker.")
                .setPositiveButton("View in Savings", (d, w) -> finish())
                .setNegativeButton("Done", (d, w) -> finish())
                .setCancelable(false)
                .show();
    }
}
