package com.bestrecharge.app.inspection;

import com.bestrecharge.app.data.local.entities.OfferEntity;
import com.bestrecharge.app.data.local.entities.RechargePlanEntity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * ProviderInspectionSession tracks the state, telemetry, and artifacts of a user-initiated
 * guided inspection session for a specific telecom provider application.
 */
public class ProviderInspectionSession implements Serializable {

    public enum Status {
        IDLE,
        INITIATED,
        CONNECTING,
        NAVIGATING,
        EXTRACTING,
        TERMS_INSPECTION,
        PAUSED_SENSITIVE_SCREEN,
        COMPLETED,
        FAILED,
        CANCELLED_BY_USER
    }

    public final String sessionId;
    public final String providerCode;
    public final String targetPackage;
    public final long startTimeMs;
    public long endTimeMs = 0;

    public Status status = Status.IDLE;
    public String currentStage = "Preparing inspection...";
    public int progressPercent = 0;
    public String failureReason = null;

    // Checkout Automation Mode
    public boolean isCheckoutMode = false;
    public String targetMobile = null;
    public double targetAmount = 0.0;
    public String recommendedPayment = null;
    public String couponCode = null;
    public boolean checkoutPageReached = false;

    public final List<String> visitedScreens = new ArrayList<>();
    public final List<OfferEntity> extractedOffers = new ArrayList<>();
    public final List<RechargePlanEntity> extractedPlans = new ArrayList<>();
    public final List<String> extractedTerms = new ArrayList<>();

    public ProviderInspectionSession(String providerCode, String targetPackage) {
        this.sessionId = "insp_" + UUID.randomUUID().toString().substring(0, 8);
        this.providerCode = providerCode != null ? providerCode.toUpperCase() : "VI";
        this.targetPackage = targetPackage;
        this.startTimeMs = System.currentTimeMillis();
        this.status = Status.INITIATED;
    }

    public boolean isActive() {
        return status == Status.INITIATED ||
                status == Status.CONNECTING ||
                status == Status.NAVIGATING ||
                status == Status.EXTRACTING ||
                status == Status.TERMS_INSPECTION ||
                status == Status.PAUSED_SENSITIVE_SCREEN;
    }

    public int getCollectedPlansCount() {
        return extractedPlans.size();
    }

    public int getCollectedOffersCount() {
        return extractedOffers.size();
    }

    public int getCollectedTermsCount() {
        return extractedTerms.size();
    }
}
