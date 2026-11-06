package com.bestrecharge.app.inspection;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;

import com.bestrecharge.app.utils.TelecomRedirectionHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * ProviderInspectionManager is the central controller coordinating user-initiated,
 * guided automatic provider inspections, consent checks, and lifecycle management.
 */
public class ProviderInspectionManager {

    private static volatile ProviderInspectionManager INSTANCE;
    private final Context context;
    private ProviderInspectionSession currentSession;
    private AccessibilityEventProcessor currentProcessor;
    private ProviderNavigationWorkflow currentWorkflow;

    private final Map<String, ProviderNavigationWorkflow> workflowRegistry = new HashMap<>();

    public static ProviderInspectionManager getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (ProviderInspectionManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ProviderInspectionManager(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    private ProviderInspectionManager(Context context) {
        this.context = context;
        workflowRegistry.put("VI", new ViInspectionAdapter());
        workflowRegistry.put("JIO", new JioInspectionAdapter());
        workflowRegistry.put("AIRTEL", new AirtelInspectionAdapter());
        workflowRegistry.put("BSNL", new BsnlInspectionAdapter());
    }

    public boolean isAccessibilityServiceEnabled() {
        if (ProviderInspectionService.isServiceRunning()) {
            return true;
        }

        String expectedServiceName = context.getPackageName() + "/" + ProviderInspectionService.class.getName();
        try {
            String enabledServices = Settings.Secure.getString(
                    context.getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            );
            if (enabledServices != null && !enabledServices.isEmpty()) {
                TextUtils.SimpleStringSplitter colonSplitter = new TextUtils.SimpleStringSplitter(':');
                colonSplitter.setString(enabledServices);
                while (colonSplitter.hasNext()) {
                    String componentName = colonSplitter.next();
                    if (componentName.equalsIgnoreCase(expectedServiceName)) {
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    public Intent getAccessibilitySettingsIntent() {
        return new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
    }

    public boolean isProviderAppInstalled(String providerCode) {
        String pkg = TelecomRedirectionHelper.getPackageForOperator(providerCode);
        try {
            context.getPackageManager().getPackageInfo(pkg, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public ProviderInspectionSession getCurrentSession() {
        return currentSession;
    }

    public synchronized ProviderInspectionSession startInspection(String providerCode) {
        String op = providerCode != null ? providerCode.toUpperCase() : "VI";
        String targetPkg = TelecomRedirectionHelper.getPackageForOperator(op);

        if (!isProviderAppInstalled(op)) {
            ProviderInspectionSession failedSession = new ProviderInspectionSession(op, targetPkg);
            InspectionStateManager.getInstance().markFailed(failedSession, op + " app is not installed on this device.");
            return failedSession;
        }

        if (!isAccessibilityServiceEnabled()) {
            ProviderInspectionSession failedSession = new ProviderInspectionSession(op, targetPkg);
            InspectionStateManager.getInstance().markFailed(failedSession, "Accessibility service is not enabled.");
            return failedSession;
        }

        currentSession = new ProviderInspectionSession(op, targetPkg);
        currentWorkflow = workflowRegistry.get(op);
        if (currentWorkflow != null) {
            currentWorkflow.reset();
            currentProcessor = new AccessibilityEventProcessor(currentWorkflow);
        }

        InspectionStateManager.getInstance().updateStage(
                currentSession,
                ProviderInspectionSession.Status.CONNECTING,
                "Launching " + TelecomRedirectionHelper.getOperatorDisplayName(op) + " application...",
                10
        );

        // Launch provider app
        launchProviderApp(targetPkg);
        return currentSession;
    }

    public synchronized void stopCurrentInspection(String reason) {
        if (currentSession != null && currentSession.isActive()) {
            InspectionStateManager.getInstance().markCancelled(currentSession);
        }
        currentSession = null;
        currentProcessor = null;
    }

    public void handleAccessibilityEvent(AccessibilityService service, AccessibilityEvent event) {
        if (currentSession != null && currentSession.isActive() && currentProcessor != null) {
            currentProcessor.processEvent(service, event, currentSession);
        }
    }

    private void launchProviderApp(String packageName) {
        try {
            Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(launchIntent);
            }
        } catch (Exception ignored) {}
    }
}
