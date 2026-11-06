package com.bestrecharge.app.inspection;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * InspectionOverlayManager draws a non-intrusive floating guidance HUD at the top of the screen
 * using WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY.
 *
 * This guides the user, shows live scan metrics, warns them not to press back while scanning,
 * and allows immediate 1-tap completion via the "Done / View Deals" button.
 */
public class InspectionOverlayManager {

    private static final String TAG = "InspectionOverlay";
    private static volatile InspectionOverlayManager INSTANCE;

    private WindowManager windowManager;
    private View overlayView;
    private TextView tvTitle;
    private TextView tvStage;
    private TextView tvWarning;
    private TextView tvStats;
    private Button btnDone;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isShowing = false;

    public static InspectionOverlayManager getInstance() {
        if (INSTANCE == null) {
            synchronized (InspectionOverlayManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new InspectionOverlayManager();
                }
            }
        }
        return INSTANCE;
    }

    private InspectionOverlayManager() {}

    public synchronized void showOverlay(AccessibilityService service, String initialStage, String opName) {
        if (service == null) return;

        mainHandler.post(() -> {
            try {
                if (isShowing && overlayView != null) {
                    updateStage(initialStage, 0, 0);
                    return;
                }

                windowManager = (WindowManager) service.getSystemService(Context.WINDOW_SERVICE);
                if (windowManager == null) return;

                overlayView = createOverlayView(service, opName);

                WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                                ? WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
                                : WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                        PixelFormat.TRANSLUCENT
                );
                params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
                params.y = dpToPx(service, 28); // Below notification status bar

                windowManager.addView(overlayView, params);
                isShowing = true;
                Log.d(TAG, "Inspection overlay attached successfully.");
            } catch (Exception e) {
                Log.w(TAG, "Could not attach accessibility overlay: " + e.getMessage());
            }
        });
    }

    public synchronized void updateStage(String stage, int plansCount, int offersCount) {
        mainHandler.post(() -> {
            try {
                if (!isShowing || overlayView == null) return;
                if (tvStage != null && stage != null) {
                    tvStage.setText(stage);
                }
                if (tvStats != null) {
                    tvStats.setText("📦 Plans: " + plansCount + "  |  🏷️ Offers & Coupons: " + offersCount);
                }
            } catch (Exception ignored) {}
        });
    }

    public synchronized void showCelebration(String message, Runnable onFinished) {
        mainHandler.post(() -> {
            try {
                if (!isShowing || overlayView == null) {
                    if (onFinished != null) onFinished.run();
                    return;
                }
                if (tvTitle != null) tvTitle.setText("🎉 Inspection Complete!");
                if (tvStage != null) tvStage.setText(message);
                if (tvWarning != null) tvWarning.setVisibility(View.GONE);
                if (btnDone != null) btnDone.setText("View Best Deals");

                // Auto dismiss after 1.5 seconds
                mainHandler.postDelayed(() -> {
                    hideOverlay();
                    if (onFinished != null) onFinished.run();
                }, 1500);
            } catch (Exception e) {
                hideOverlay();
                if (onFinished != null) onFinished.run();
            }
        });
    }

    public synchronized void hideOverlay() {
        mainHandler.post(() -> {
            try {
                if (isShowing && windowManager != null && overlayView != null) {
                    windowManager.removeView(overlayView);
                }
            } catch (Exception ignored) {
            } finally {
                overlayView = null;
                isShowing = false;
            }
        });
    }

    private View createOverlayView(AccessibilityService service, String opName) {
        LinearLayout root = new LinearLayout(service);
        root.setOrientation(LinearLayout.VERTICAL);
        int padH = dpToPx(service, 16);
        int padV = dpToPx(service, 12);
        root.setPadding(padH, padV, padH, padV);

        // Modern dark rounded background with green border
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#1E293B")); // Slate dark
        bg.setCornerRadius(dpToPx(service, 16));
        bg.setStroke(dpToPx(service, 2), Color.parseColor("#10B981")); // Emerald border
        root.setBackground(bg);

        // Header row
        LinearLayout headerRow = new LinearLayout(service);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        tvTitle = new TextView(service);
        tvTitle.setText("⚡ BestRecharge Auto-Inspection");
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        headerRow.addView(tvTitle, titleParams);

        btnDone = new Button(service);
        btnDone.setText("Done / View Deals");
        btnDone.setTextColor(Color.WHITE);
        btnDone.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btnDone.setTypeface(null, android.graphics.Typeface.BOLD);
        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(Color.parseColor("#059669")); // Emerald green
        btnBg.setCornerRadius(dpToPx(service, 8));
        btnDone.setBackground(btnBg);
        btnDone.setPadding(dpToPx(service, 12), dpToPx(service, 4), dpToPx(service, 12), dpToPx(service, 4));

        btnDone.setOnClickListener(v -> {
            ProviderInspectionSession session = ProviderInspectionManager.getInstance(service).getCurrentSession();
            if (session != null) {
                InspectionStateManager.getInstance().markCompleted(session);
            }
            hideOverlay();
            bringBestRechargeToForeground(service);
        });
        headerRow.addView(btnDone);
        root.addView(headerRow);

        // Stage text
        tvStage = new TextView(service);
        tvStage.setText("Scanning " + (opName != null ? opName : "provider") + " plans, offers & coupons...");
        tvStage.setTextColor(Color.parseColor("#94A3B8")); // Slate 400
        tvStage.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvStage.setPadding(0, dpToPx(service, 6), 0, 0);
        root.addView(tvStage);

        // Warning text: Do NOT press back
        tvWarning = new TextView(service);
        tvWarning.setText("⚠️ Scanning in progress. Please do not press Back.");
        tvWarning.setTextColor(Color.parseColor("#FBBF24")); // Amber 400
        tvWarning.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvWarning.setTypeface(null, android.graphics.Typeface.BOLD);
        tvWarning.setPadding(0, dpToPx(service, 4), 0, 0);
        root.addView(tvWarning);

        // Stats badge
        tvStats = new TextView(service);
        tvStats.setText("📦 Plans: 0  |  🏷️ Offers & Coupons: 0");
        tvStats.setTextColor(Color.parseColor("#34D399")); // Emerald 400
        tvStats.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvStats.setPadding(0, dpToPx(service, 4), 0, 0);
        root.addView(tvStats);

        return root;
    }

    private void bringBestRechargeToForeground(AccessibilityService service) {
        try {
            android.content.Intent launchIntent = service.getPackageManager()
                    .getLaunchIntentForPackage(service.getPackageName());
            if (launchIntent != null) {
                launchIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK |
                        android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP);
                service.startActivity(launchIntent);
            }
        } catch (Exception ignored) {}
    }

    private int dpToPx(Context context, int dp) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        return (int) (dp * dm.density + 0.5f);
    }
}
