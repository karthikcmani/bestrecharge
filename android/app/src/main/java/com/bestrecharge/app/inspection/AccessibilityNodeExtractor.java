package com.bestrecharge.app.inspection;

import android.graphics.Rect;
import android.os.Build;
import android.view.accessibility.AccessibilityNodeInfo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * AccessibilityNodeExtractor performs bounded, recursive parsing of Android AccessibilityNodeInfo
 * hierarchies. It extracts structured textual and structural metadata while detecting sensitive screens
 * (passwords, OTPs, UPI PINs) to safeguard user privacy.
 */
public class AccessibilityNodeExtractor {

    public static class ExtractedElement implements Serializable {
        public String text = "";
        public String contentDescription = "";
        public String resourceId = "";
        public String className = "";
        public boolean isClickable = false;
        public boolean isScrollable = false;
        public boolean isEditable = false;
        public boolean isPassword = false;
        public Rect bounds = new Rect();

        public String getEffectiveLabel() {
            if (text != null && !text.trim().isEmpty()) {
                return text.trim();
            }
            if (contentDescription != null && !contentDescription.trim().isEmpty()) {
                return contentDescription.trim();
            }
            return "";
        }
    }

    public static class ScreenSnapshot implements Serializable {
        public String packageName = "";
        public String rootClassName = "";
        public boolean isSensitiveAuthScreen = false;
        public String sensitiveReason = "";
        public final List<ExtractedElement> elements = new ArrayList<>();

        public List<String> getAllVisibleTexts() {
            List<String> list = new ArrayList<>();
            for (ExtractedElement e : elements) {
                String label = e.getEffectiveLabel();
                if (!label.isEmpty()) {
                    list.add(label);
                }
            }
            return list;
        }

        public ExtractedElement findElementByText(String targetText, boolean ignoreCase) {
            if (targetText == null) return null;
            for (ExtractedElement e : elements) {
                String label = e.getEffectiveLabel();
                if (ignoreCase ? label.equalsIgnoreCase(targetText) : label.contains(targetText)) {
                    return e;
                }
            }
            return null;
        }

        public ExtractedElement findElementContaining(String substring) {
            if (substring == null) return null;
            String lower = substring.toLowerCase();
            for (ExtractedElement e : elements) {
                if (e.getEffectiveLabel().toLowerCase().contains(lower)) {
                    return e;
                }
            }
            return null;
        }
    }

    private static final int MAX_DEPTH = 35;
    private static final int MAX_ELEMENTS = 300;

    public static ScreenSnapshot extractSnapshot(AccessibilityNodeInfo root) {
        ScreenSnapshot snapshot = new ScreenSnapshot();
        if (root == null) return snapshot;

        try {
            CharSequence pkg = root.getPackageName();
            snapshot.packageName = pkg != null ? pkg.toString() : "";
            CharSequence rootCls = root.getClassName();
            snapshot.rootClassName = rootCls != null ? rootCls.toString() : "";

            traverseRecursively(root, snapshot, 0);
        } catch (Exception ignored) {}

        return snapshot;
    }

    private static void traverseRecursively(AccessibilityNodeInfo node, ScreenSnapshot snapshot, int depth) {
        if (node == null || depth > MAX_DEPTH || snapshot.elements.size() >= MAX_ELEMENTS) {
            return;
        }

        ExtractedElement elem = new ExtractedElement();
        CharSequence text = node.getText();
        if (text != null) elem.text = text.toString();

        CharSequence cd = node.getContentDescription();
        if (cd != null) elem.contentDescription = cd.toString();

        CharSequence resId = node.getViewIdResourceName();
        if (resId != null) elem.resourceId = resId.toString();

        CharSequence cls = node.getClassName();
        if (cls != null) elem.className = cls.toString();

        elem.isClickable = node.isClickable();
        elem.isScrollable = node.isScrollable();
        elem.isEditable = node.isEditable();
        elem.isPassword = node.isPassword();

        node.getBoundsInScreen(elem.bounds);

        // Check for sensitive auth or banking screens (Section 6.5 & 14)
        String combinedLabel = (elem.text + " " + elem.contentDescription + " " + elem.resourceId).toLowerCase();
        if (elem.isPassword || combinedLabel.contains("upi pin") || combinedLabel.contains("enter otp") ||
                combinedLabel.contains("mpin") || combinedLabel.contains("netbanking password") ||
                combinedLabel.contains("cvv") || combinedLabel.contains("card expiry")) {
            snapshot.isSensitiveAuthScreen = true;
            snapshot.sensitiveReason = "Sensitive credential / payment field detected";
        }

        // Only add elements that contain visible text or actionable controls
        if (!elem.getEffectiveLabel().isEmpty() || elem.isClickable || elem.isScrollable) {
            snapshot.elements.add(elem);
        }

        int childCount = node.getChildCount();
        for (int i = 0; i < childCount; i++) {
            AccessibilityNodeInfo child = null;
            try {
                child = node.getChild(i);
                if (child != null) {
                    traverseRecursively(child, snapshot, depth + 1);
                }
            } catch (Exception ignored) {
            } finally {
                if (child != null) {
                    try {
                        child.recycle();
                    } catch (Exception ignored) {}
                }
            }
        }
    }
}
