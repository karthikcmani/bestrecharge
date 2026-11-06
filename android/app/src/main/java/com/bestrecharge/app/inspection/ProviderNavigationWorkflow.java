package com.bestrecharge.app.inspection;

import android.accessibilityservice.AccessibilityService;

/**
 * ProviderNavigationWorkflow defines the stateful contract for navigating and extracting
 * data inside a specific telecom provider application.
 */
public interface ProviderNavigationWorkflow {
    String getProviderCode();
    String getTargetPackage();
    boolean onScreenChanged(
            AccessibilityService service,
            ProviderInspectionSession session,
            AccessibilityNodeExtractor.ScreenSnapshot snapshot
    );
    void reset();
}
