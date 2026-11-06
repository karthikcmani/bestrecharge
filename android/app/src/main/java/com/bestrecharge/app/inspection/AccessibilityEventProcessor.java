package com.bestrecharge.app.inspection;

import android.accessibilityservice.AccessibilityService;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

/**
 * AccessibilityEventProcessor filters, validates, and dispatches accessibility events
 * strictly for user-authorized, active provider inspection sessions.
 */
public class AccessibilityEventProcessor {

    private static final String TAG = "A11yEventProcessor";
    private static final long DEBOUNCE_MS = 350;

    private long lastEventTime = 0;
    private final ProviderNavigationWorkflow activeWorkflow;

    public AccessibilityEventProcessor(ProviderNavigationWorkflow workflow) {
        this.activeWorkflow = workflow;
    }

    public void processEvent(AccessibilityService service, AccessibilityEvent event, ProviderInspectionSession session) {
        if (service == null || event == null || session == null || !session.isActive()) {
            return;
        }

        CharSequence pkg = event.getPackageName();
        String currentPkg = pkg != null ? pkg.toString() : "";

        // Security check (Section 6.3 & 14): strictly reject any package outside session allowlist
        if (!session.targetPackage.equals(currentPkg)) {
            Log.d(TAG, "Unrelated package active (" + currentPkg + "). Pausing inspection.");
            InspectionStateManager.getInstance().updateStage(session,
                    ProviderInspectionSession.Status.PAUSED_SENSITIVE_SCREEN,
                    "Inspection paused (outside " + session.providerCode + " app)",
                    session.progressPercent);
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastEventTime < DEBOUNCE_MS) {
            return;
        }
        lastEventTime = now;

        AccessibilityNodeInfo root = service.getRootInActiveWindow();
        if (root == null) return;

        try {
            AccessibilityNodeExtractor.ScreenSnapshot snapshot = AccessibilityNodeExtractor.extractSnapshot(root);
            if (activeWorkflow != null) {
                activeWorkflow.onScreenChanged(service, session, snapshot);
            }
        } finally {
            try {
                root.recycle();
            } catch (Exception ignored) {}
        }
    }
}
