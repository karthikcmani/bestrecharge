package com.bestrecharge.app.ui.inspection;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bestrecharge.app.databinding.ActivityProviderIntelligenceBinding;
import com.bestrecharge.app.inspection.InspectionStateManager;
import com.bestrecharge.app.inspection.ProviderInspectionManager;
import com.bestrecharge.app.inspection.ProviderInspectionSession;
import com.bestrecharge.app.ui.main.MainActivity;

/**
 * ProviderIntelligenceActivity provides the user-supervised control centre for
 * inspecting installed telecom provider applications, viewing real-time extraction
 * metrics, and onboarding accessibility permissions.
 */
public class ProviderIntelligenceActivity extends AppCompatActivity {

    private ActivityProviderIntelligenceBinding binding;
    private ProviderInspectionManager manager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProviderIntelligenceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Handle edge-to-edge window insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            binding.containerInspection.setPadding(
                    binding.containerInspection.getPaddingLeft(),
                    insets.top,
                    binding.containerInspection.getPaddingRight(),
                    binding.containerInspection.getPaddingBottom()
            );
            return windowInsets;
        });

        manager = ProviderInspectionManager.getInstance(this);

        binding.btnInspectionBack.setOnClickListener(v -> finish());

        binding.btnEnableAccessibility.setOnClickListener(v -> {
            startActivity(manager.getAccessibilitySettingsIntent());
            Toast.makeText(this, "Find 'BestRecharge' in Accessibility Settings and turn it ON", Toast.LENGTH_LONG).show();
        });

        // Provider Inspection Buttons
        binding.btnInspectVi.setOnClickListener(v -> triggerInspection("VI"));
        binding.btnInspectJio.setOnClickListener(v -> triggerInspection("JIO"));
        binding.btnInspectAirtel.setOnClickListener(v -> triggerInspection("AIRTEL"));
        binding.btnInspectBsnl.setOnClickListener(v -> triggerInspection("BSNL"));

        binding.btnStopInspection.setOnClickListener(v -> {
            manager.stopCurrentInspection("Stopped by user");
            Toast.makeText(this, "Inspection stopped", Toast.LENGTH_SHORT).show();
        });

        binding.btnViewExtractedDeals.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        binding.btnInspectAgain.setOnClickListener(v -> {
            binding.cardInspectionSummary.setVisibility(View.GONE);
            updateProviderCards();
        });

        // Observe live inspection session
        InspectionStateManager.getInstance().getSessionLiveData().observe(this, this::renderSessionState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateProviderCards();
    }

    private void updateProviderCards() {
        boolean a11yEnabled = manager.isAccessibilityServiceEnabled();
        binding.cardA11yOnboarding.setVisibility(a11yEnabled ? View.GONE : View.VISIBLE);

        // Vi Status
        boolean viInstalled = manager.isProviderAppInstalled("VI");
        binding.tvViStatus.setText(viInstalled ?
                (a11yEnabled ? "Application: Installed • Inspection Ready" : "Application: Installed • Requires Accessibility") :
                "Application: Not Installed (Tariff sourcing active)");
        binding.tvViStatus.setTextColor(getResources().getColor(
                viInstalled ? com.bestrecharge.app.R.color.savings_green_dark : com.bestrecharge.app.R.color.on_surface_muted, null
        ));
        binding.btnInspectVi.setEnabled(viInstalled);

        // Jio Status
        boolean jioInstalled = manager.isProviderAppInstalled("JIO");
        binding.tvJioStatus.setText(jioInstalled ? "Application: Installed • Inspection Ready" : "Application: Not Installed");
        binding.btnInspectJio.setEnabled(jioInstalled);

        // Airtel Status
        boolean airtelInstalled = manager.isProviderAppInstalled("AIRTEL");
        binding.tvAirtelStatus.setText(airtelInstalled ? "Application: Installed • Inspection Ready" : "Application: Not Installed");
        binding.btnInspectAirtel.setEnabled(airtelInstalled);

        // BSNL Status
        boolean bsnlInstalled = manager.isProviderAppInstalled("BSNL");
        binding.tvBsnlStatus.setText(bsnlInstalled ? "Application: Installed • Inspection Ready" : "Application: Not Installed");
        binding.btnInspectBsnl.setEnabled(bsnlInstalled);
    }

    private void triggerInspection(String providerCode) {
        if (!manager.isAccessibilityServiceEnabled()) {
            Toast.makeText(this, "Please enable the BestRecharge Accessibility Service first", Toast.LENGTH_LONG).show();
            startActivity(manager.getAccessibilitySettingsIntent());
            return;
        }

        ProviderInspectionSession session = manager.startInspection(providerCode);
        if (session != null && session.isActive()) {
            Toast.makeText(this, "Opening " + providerCode + " app for guided inspection...", Toast.LENGTH_SHORT).show();
        }
    }

    private void renderSessionState(ProviderInspectionSession session) {
        if (session == null) {
            binding.cardInspectionProgress.setVisibility(View.GONE);
            return;
        }

        if (session.isActive()) {
            binding.cardInspectionProgress.setVisibility(View.VISIBLE);
            binding.cardInspectionSummary.setVisibility(View.GONE);

            binding.tvProgressProviderTitle.setText("Inspecting " + session.providerCode + " Offers");
            binding.tvProgressPercentage.setText(session.progressPercent + "%");
            binding.progressIndicator.setProgress(session.progressPercent);
            binding.tvProgressStageDesc.setText(session.currentStage);

            binding.tvMetricPlans.setText("📦 Plans: " + session.getCollectedPlansCount());
            binding.tvMetricOffers.setText("🏷️ Offers: " + session.getCollectedOffersCount());
            binding.tvMetricTerms.setText("📜 Terms: " + session.getCollectedTermsCount());
        } else if (session.status == ProviderInspectionSession.Status.COMPLETED) {
            binding.cardInspectionProgress.setVisibility(View.GONE);
            binding.cardInspectionSummary.setVisibility(View.VISIBLE);

            binding.tvSummaryDetails.setText("Successfully inspected " + session.providerCode + ":\n" +
                    "• " + session.getCollectedPlansCount() + " recharge plans\n" +
                    "• " + session.getCollectedOffersCount() + " payment offers\n" +
                    "• " + session.getCollectedTermsCount() + " promotional terms & conditions extracted.");
        } else if (session.status == ProviderInspectionSession.Status.FAILED) {
            binding.cardInspectionProgress.setVisibility(View.GONE);
            Toast.makeText(this, "Inspection failed: " + session.failureReason, Toast.LENGTH_LONG).show();
        } else {
            binding.cardInspectionProgress.setVisibility(View.GONE);
        }
    }
}
