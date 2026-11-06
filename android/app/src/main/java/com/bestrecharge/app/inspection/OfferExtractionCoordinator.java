package com.bestrecharge.app.inspection;

import android.content.Context;

import com.bestrecharge.app.data.local.AppDatabase;
import com.bestrecharge.app.data.local.entities.OfferEntity;
import com.bestrecharge.app.data.local.entities.RechargePlanEntity;

import java.util.List;
import java.util.concurrent.Executors;

/**
 * OfferExtractionCoordinator saves newly inspected plans and offers to Room database,
 * deduplicating records and updating the active ProviderInspectionSession statistics.
 */
public class OfferExtractionCoordinator {

    public static void saveInspectedData(
            Context context,
            ProviderInspectionSession session,
            List<RechargePlanEntity> plans,
            List<OfferEntity> offers,
            String termsText
    ) {
        if (context == null || session == null) return;

        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(context);

            if (plans != null && !plans.isEmpty()) {
                for (RechargePlanEntity p : plans) {
                    db.rechargePlanDao().insert(p);
                    if (!session.extractedPlans.contains(p)) {
                        session.extractedPlans.add(p);
                    }
                }
            }

            if (offers != null && !offers.isEmpty()) {
                for (OfferEntity o : offers) {
                    db.offerDao().insert(o);
                    if (!session.extractedOffers.contains(o)) {
                        session.extractedOffers.add(o);
                    }
                }
            }

            if (termsText != null && !termsText.trim().isEmpty()) {
                if (!session.extractedTerms.contains(termsText)) {
                    session.extractedTerms.add(termsText);
                }
            }

            InspectionStateManager.getInstance().updateSession(session);
        });
    }
}
