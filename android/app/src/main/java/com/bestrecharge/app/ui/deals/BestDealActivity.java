package com.bestrecharge.app.ui.deals;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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

        binding.tvDetailExplanation.setText(deal.explanation);
        binding.btnContinueToRecharge.setText(deal.actionLabel != null ? deal.actionLabel : "Continue to Recharge");

        binding.cardAltBhim.setOnClickListener(v -> {
            Toast.makeText(this, "Selected BHIM alternative deal", Toast.LENGTH_SHORT).show();
        });
        binding.cardAltPhonepe.setOnClickListener(v -> {
            Toast.makeText(this, "Selected PhonePe alternative deal", Toast.LENGTH_SHORT).show();
        });
    }

    private void recordAndOpenPayment(DealBreakdown deal) {
        // 1. Log transaction in local Room savings history
        String mobile = primarySim != null ? primarySim.mobileNumber : "98478 37607";
        String operator = activePlan != null ? activePlan.operatorName : "Vi";
        String circle = primarySim != null ? primarySim.circleName : "Delhi NCR";
        String planName = activePlan != null ? activePlan.description : "Mobile Recharge";
        String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date());

        SavingsRecordEntity record = new SavingsRecordEntity(
                "rec_" + UUID.randomUUID().toString().substring(0, 8),
                mobile, operator, circle, planName,
                deal.originalMrp, deal.operatorDiscount, deal.immediatePayable,
                deal.recommendedPaymentProvider,
                deal.stackedOffers != null && !deal.stackedOffers.isEmpty() ? deal.stackedOffers.get(0) : "Smart Offer",
                deal.guaranteedCashback > 0 ? deal.guaranteedCashback : deal.potentialCashback,
                0.0, "PENDING", deal.potentialEffectiveCost, dateStr,
                "Initiated via BestRecharge intelligence engine."
        );

        new SavingsRepository(getApplication()).insertRecord(record);

        // 2. Open payment app via intent
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(deal.actionIntentUri));
            startActivity(intent);
        } catch (Exception e) {
            // Fallback to web portal if specific UPI app intent is not installed
            Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.myvi.in"));
            try {
                startActivity(webIntent);
            } catch (Exception ignored) {
                Toast.makeText(this, "Please open " + deal.recommendedPaymentProvider + " to complete payment.", Toast.LENGTH_LONG).show();
            }
        }

        Toast.makeText(this, "Recharge initiated. Transaction logged to Savings Tracker!", Toast.LENGTH_SHORT).show();
        finish();
    }
}
