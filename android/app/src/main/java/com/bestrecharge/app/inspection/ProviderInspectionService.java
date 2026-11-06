package com.bestrecharge.app.inspection;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

/**
 * ProviderInspectionService is the dedicated, narrowly scoped Android AccessibilityService
 * used exclusively for user-initiated guided provider inspections.
 */
public class ProviderInspectionService extends AccessibilityService {

    private static final String TAG = "ProviderInspectionSvc";
    private static volatile ProviderInspectionService runningInstance;

    public static boolean isServiceRunning() {
        return runningInstance != null;
    }

    public static ProviderInspectionService getInstance() {
        return runningInstance;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        runningInstance = this;
        Log.i(TAG, "ProviderInspectionService connected and ready.");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        ProviderInspectionManager manager = ProviderInspectionManager.getInstance(getApplicationContext());
        manager.handleAccessibilityEvent(this, event);
    }

    @Override
    public void onInterrupt() {
        Log.w(TAG, "ProviderInspectionService interrupted.");
        ProviderInspectionManager manager = ProviderInspectionManager.getInstance(getApplicationContext());
        manager.stopCurrentInspection("Accessibility service interrupted");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        runningInstance = null;
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        runningInstance = null;
    }
}
