package com.bestrecharge.app.inspection;

import android.accessibilityservice.AccessibilityService;

import com.bestrecharge.app.data.local.entities.OfferEntity;
import com.bestrecharge.app.data.local.entities.RechargePlanEntity;

import java.util.List;

/**
 * BsnlInspectionAdapter coordinates guided inspection of the BSNL self-care app (in.co.bsnl).
 */
public class BsnlInspectionAdapter implements ProviderNavigationWorkflow {

    public static final String PKG_BSNL = "in.co.bsnl";
    private boolean completed = false;

    @Override
    public String getProviderCode() {
        return "BSNL";
    }

    @Override
    public String getTargetPackage() {
        return PKG_BSNL;
    }

    @Override
    public void reset() {
        completed = false;
    }

    @Override
    public boolean onScreenChanged(
            AccessibilityService service,
            ProviderInspectionSession session,
            AccessibilityNodeExtractor.ScreenSnapshot snapshot
    ) {
        if (service == null || session == null || snapshot == null) return false;

        if (snapshot.isSensitiveAuthScreen) {
            InspectionStateManager.getInstance().markSensitiveScreenPaused(session, snapshot.sensitiveReason);
            return true;
        }

        if (!completed) {
            InspectionStateManager.getInstance().updateStage(session,
                    ProviderInspectionSession.Status.EXTRACTING,
                    "Reading BSNL official tariffs...", 50);

            List<RechargePlanEntity> plans = ScreenContentParser.extractPlans(snapshot, "BSNL");
            List<OfferEntity> offers = ScreenContentParser.extractOffers(snapshot, "BSNL");

            if (!plans.isEmpty() || !offers.isEmpty()) {
                OfferExtractionCoordinator.saveInspectedData(service, session, plans, offers, null);
                completed = true;
                InspectionStateManager.getInstance().markCompleted(session);
                return true;
            }
        }
        return false;
    }
}
