package com.bestrecharge.app.utils;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

/**
 * TelecomRedirectionHelper provides intelligent deep-linking and app-linking
 * to telecom operator checkout pages (Vi, Jio, Airtel, BSNL).
 *
 * It passes the target mobile number and plan recharge amount directly via
 * verified App Links, custom URI schemes, and Intent extras so the provider app
 * opens directly onto the recharge checkout screen rather than just the generic launcher.
 */
public class TelecomRedirectionHelper {

    private static final String TAG = "TelecomRedirection";

    public static final String PKG_VI = "com.mventus.selfcare.activity";
    public static final String PKG_JIO = "com.jio.myjio";
    public static final String PKG_AIRTEL = "com.myairtelapp";
    public static final String PKG_BSNL = "in.co.bsnl";

    /**
     * Redirect directly to the provider's recharge checkout page for a specific mobile number and amount.
     */
    public static boolean redirectToRechargeCheckout(
            Context context,
            String operatorCode,
            String rawMobileNumber,
            double amount,
            String promoInstruction
    ) {
        if (context == null) return false;

        // Clean the mobile number to 10 digits
        String mobile = clean10DigitNumber(rawMobileNumber);
        String op = operatorCode != null ? operatorCode.trim().toUpperCase() : "VI";
        int amtInt = (int) Math.round(amount);

        // Copy any promo instructions or coupon codes to clipboard for effortless user pasting
        if (promoInstruction != null && !promoInstruction.trim().isEmpty()) {
            copyToClipboard(context, "Recharge Offer Details", promoInstruction);
        }

        String targetPackage;
        String webCheckoutUrl;
        String customSchemeUri;

        if ("JIO".equals(op)) {
            targetPackage = PKG_JIO;
            webCheckoutUrl = "https://www.jio.com/selfcare/recharge/mobility/?msisdn=" + mobile + "&amount=" + amtInt;
            customSchemeUri = "jio://com.jio.myjio/recharge?msisdn=" + mobile + "&amount=" + amtInt;
        } else if ("AIRTEL".equals(op)) {
            targetPackage = PKG_AIRTEL;
            webCheckoutUrl = "https://www.airtel.in/recharge-online?number=" + mobile + "&amount=" + amtInt;
            customSchemeUri = "airtel://recharge?number=" + mobile + "&amount=" + amtInt;
        } else if ("BSNL".equals(op)) {
            targetPackage = PKG_BSNL;
            webCheckoutUrl = "https://portal2.bsnl.in/myportal/tariffs.do";
            customSchemeUri = null;
        } else {
            // Default: Vi (Vodafone Idea)
            targetPackage = PKG_VI;
            webCheckoutUrl = "https://www.myvi.in/prepaid/online-mobile-recharge?mobileNumber=" + mobile + "&amount=" + amtInt;
            customSchemeUri = "myvi://prepaid/recharge?mobileNumber=" + mobile + "&amount=" + amtInt;
        }

        boolean isAppInstalled = isPackageInstalled(context, targetPackage);
        Log.d(TAG, "Redirecting for " + op + " (" + targetPackage + "), installed: " + isAppInstalled);

        // 1. Attempt targeted App Link / Deep Link directly to checkout page
        if (isAppInstalled) {
            // Try standard HTTP App Link with package targeting
            try {
                Intent deepLinkIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webCheckoutUrl));
                deepLinkIntent.setPackage(targetPackage);
                deepLinkIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                populateExtras(deepLinkIntent, mobile, amtInt);

                if (canResolveIntent(context, deepLinkIntent)) {
                    context.startActivity(deepLinkIntent);
                    showToast(context, "Opening " + getOperatorDisplayName(op) + " checkout for ₹" + amtInt);
                    return true;
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed launching App Link with package: " + e.getMessage());
            }

            // Try custom scheme if App Link did not resolve
            if (customSchemeUri != null) {
                try {
                    Intent schemeIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(customSchemeUri));
                    schemeIntent.setPackage(targetPackage);
                    schemeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    populateExtras(schemeIntent, mobile, amtInt);

                    if (canResolveIntent(context, schemeIntent)) {
                        context.startActivity(schemeIntent);
                        showToast(context, "Opening " + getOperatorDisplayName(op) + " checkout for ₹" + amtInt);
                        return true;
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Failed launching custom scheme: " + e.getMessage());
                }
            }

            // Fallback: Launch the installed app's main launcher with intent extras preloaded
            try {
                Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(targetPackage);
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    populateExtras(launchIntent, mobile, amtInt);
                    context.startActivity(launchIntent);
                    showToast(context, "Opening " + getOperatorDisplayName(op) + " for number " + mobile);
                    return true;
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed launching root app intent: " + e.getMessage());
            }
        }

        // 2. Fallback to Official Web Checkout Portal (e.g. if app is not installed or unable to handle intent)
        try {
            Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webCheckoutUrl));
            webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(webIntent);
            showToast(context, "Opening official online checkout for " + mobile);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Cannot launch web portal: " + e.getMessage());
            showToast(context, "Could not open recharge portal");
            return false;
        }
    }

    private static void populateExtras(Intent intent, String mobile, int amount) {
        intent.putExtra("mobileNumber", mobile);
        intent.putExtra("mobile", mobile);
        intent.putExtra("msisdn", mobile);
        intent.putExtra("number", mobile);
        intent.putExtra("amount", String.valueOf(amount));
        intent.putExtra("recharge_amount", amount);
        intent.putExtra("plan_amount", amount);
        intent.putExtra("source", "BestRecharge");
    }

    private static boolean isPackageInstalled(Context context, String packageName) {
        try {
            context.getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private static boolean canResolveIntent(Context context, Intent intent) {
        return !context.getPackageManager().queryIntentActivities(intent, 0).isEmpty();
    }

    public static String clean10DigitNumber(String input) {
        if (input == null) return "9847837607";
        String digits = input.replaceAll("[^0-9]", "");
        if (digits.length() > 10) {
            digits = digits.substring(digits.length() - 10);
        }
        return digits.isEmpty() ? "9847837607" : digits;
    }

    public static String getOperatorDisplayName(String opCode) {
        if ("JIO".equalsIgnoreCase(opCode)) return "MyJio";
        if ("AIRTEL".equalsIgnoreCase(opCode)) return "Airtel Thanks";
        if ("BSNL".equalsIgnoreCase(opCode)) return "BSNL";
        return "Vi App";
    }

    private static void copyToClipboard(Context context, String label, String text) {
        try {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                ClipData clip = ClipData.newPlainText(label, text);
                clipboard.setPrimaryClip(clip);
            }
        } catch (Exception e) {
            Log.w(TAG, "Clipboard copy error: " + e.getMessage());
        }
    }

    private static void showToast(Context context, String message) {
        try {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        } catch (Exception ignored) {}
    }
}
