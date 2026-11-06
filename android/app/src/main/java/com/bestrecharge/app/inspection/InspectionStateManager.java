package com.bestrecharge.app.inspection;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * InspectionStateManager maintains the observable state machine for guided provider inspections,
 * providing real-time stage updates, error propagation, and pause/resume control.
 */
public class InspectionStateManager {

    public interface StateChangeListener {
        void onStateChanged(ProviderInspectionSession session);
    }

    private static volatile InspectionStateManager INSTANCE;
    private final MutableLiveData<ProviderInspectionSession> sessionLiveData = new MutableLiveData<>();
    private StateChangeListener listener;

    public static InspectionStateManager getInstance() {
        if (INSTANCE == null) {
            synchronized (InspectionStateManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new InspectionStateManager();
                }
            }
        }
        return INSTANCE;
    }

    public void setStateChangeListener(StateChangeListener listener) {
        this.listener = listener;
    }

    public LiveData<ProviderInspectionSession> getSessionLiveData() {
        return sessionLiveData;
    }

    public void updateSession(ProviderInspectionSession session) {
        if (session != null) {
            sessionLiveData.postValue(session);
            if (listener != null) {
                listener.onStateChanged(session);
            }
        }
    }

    public void updateStage(ProviderInspectionSession session, ProviderInspectionSession.Status status,
                            String stageText, int progressPercent) {
        if (session == null) return;
        session.status = status;
        session.currentStage = stageText;
        session.progressPercent = Math.max(0, Math.min(100, progressPercent));
        updateSession(session);
    }

    public void markSensitiveScreenPaused(ProviderInspectionSession session, String reason) {
        if (session == null) return;
        session.status = ProviderInspectionSession.Status.PAUSED_SENSITIVE_SCREEN;
        session.currentStage = "Paused: " + reason + " (Manual user handling required)";
        updateSession(session);
    }

    public void markCompleted(ProviderInspectionSession session) {
        if (session == null) return;
        session.status = ProviderInspectionSession.Status.COMPLETED;
        session.endTimeMs = System.currentTimeMillis();
        session.progressPercent = 100;
        session.currentStage = "Inspection completed successfully.";
        updateSession(session);
    }

    public void markFailed(ProviderInspectionSession session, String reason) {
        if (session == null) return;
        session.status = ProviderInspectionSession.Status.FAILED;
        session.endTimeMs = System.currentTimeMillis();
        session.failureReason = reason;
        session.currentStage = "Inspection failed: " + reason;
        updateSession(session);
    }

    public void markCancelled(ProviderInspectionSession session) {
        if (session == null) return;
        session.status = ProviderInspectionSession.Status.CANCELLED_BY_USER;
        session.endTimeMs = System.currentTimeMillis();
        session.currentStage = "Inspection cancelled by user.";
        updateSession(session);
    }
}
