package com.bestrecharge.app.ocr;

import android.graphics.Bitmap;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScreenshotOcrManager {

    public interface OcrCallback {
        void onSuccess(ParsedOffer parsedOffer, String rawText);
        void onError(Exception e);
    }

    public static void processScreenshot(Bitmap bitmap, OcrCallback callback) {
        if (bitmap == null) {
            callback.onError(new IllegalArgumentException("Bitmap cannot be null"));
            return;
        }

        try {
            InputImage image = InputImage.fromBitmap(bitmap, 0);
            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

            recognizer.process(image)
                    .addOnSuccessListener(new OnSuccessListener<Text>() {
                        @Override
                        public void onSuccess(Text text) {
                            String rawText = text.getText();
                            ParsedOffer parsed = parseExtractedText(rawText);
                            callback.onSuccess(parsed, rawText);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(Exception e) {
                            callback.onError(e);
                        }
                    });
        } catch (Exception e) {
            callback.onError(e);
        }
    }

    public static ParsedOffer parseExtractedText(String rawText) {
        ParsedOffer offer = new ParsedOffer();
        offer.termsAndConditions = rawText;

        String text = rawText.replace("\n", " ").replace("₹", " Rs ").replace("Rs.", " Rs ");
        String textLower = text.toLowerCase();

        // 1. Provider
        if (textLower.contains("paytm")) {
            offer.provider = "Paytm";
            offer.paymentMethodRequired = "Paytm UPI";
        } else if (textLower.contains("bhim")) {
            offer.provider = "BHIM";
            offer.paymentMethodRequired = "BHIM Payments App";
        } else if (textLower.contains("phonepe")) {
            offer.provider = "PhonePe";
            offer.paymentMethodRequired = "PhonePe UPI";
        } else if (textLower.contains("google pay") || textLower.contains("gpay")) {
            offer.provider = "Google Pay";
            offer.paymentMethodRequired = "Google Pay UPI";
        } else if (textLower.contains("amazon pay")) {
            offer.provider = "Amazon Pay";
            offer.paymentMethodRequired = "Amazon Pay";
        } else if (textLower.contains("vi upi") || textLower.contains("app exclusive") || textLower.contains("saved ₹15")) {
            offer.provider = "Vi";
            offer.category = "OPERATOR";
        }

        // 2. Instant Saved vs Cashback
        Pattern savePattern = Pattern.compile("(?:saved|save|off)\\s*(?:rs|rs\\.)?\\s*(\\d+)");
        Matcher saveMatcher = savePattern.matcher(textLower);
        if (saveMatcher.find()) {
            try {
                offer.discountAmount = Double.parseDouble(saveMatcher.group(1));
                offer.discountType = "FLAT_DISCOUNT";
            } catch (Exception ignored) {}
        }

        // 3. Cashback Amount
        Pattern cbPattern = Pattern.compile("(?:cashback|cash back)[^\\d]{0,20}(?:rs|rs\\.)?\\s*(\\d+)");
        Matcher cbMatcher = cbPattern.matcher(textLower);
        if (cbMatcher.find()) {
            try {
                offer.cashbackAmount = Double.parseDouble(cbMatcher.group(1));
            } catch (Exception ignored) {}
        } else {
            Pattern cbAltPattern = Pattern.compile("(?:rs|rs\\.)?\\s*(\\d+)\\s*(?:cashback|cash back)");
            Matcher cbAltMatcher = cbAltPattern.matcher(textLower);
            if (cbAltMatcher.find()) {
                try {
                    offer.cashbackAmount = Double.parseDouble(cbAltMatcher.group(1));
                } catch (Exception ignored) {}
            }
        }

        if (textLower.contains("upto") || textLower.contains("up to")) {
            offer.discountType = "UPTO_CASHBACK";
        } else if (offer.cashbackAmount > 0) {
            offer.discountType = "FLAT_CASHBACK";
        }

        // 4. Minimum Transaction Value
        Pattern minPattern = Pattern.compile("min(?:imum)?\\s*transaction[^\\d]{0,20}(?:rs|rs\\.)?\\s*(\\d+)");
        Matcher minMatcher = minPattern.matcher(textLower);
        if (minMatcher.find()) {
            try {
                offer.minTransactionAmount = Double.parseDouble(minMatcher.group(1));
            } catch (Exception ignored) {}
        } else {
            Pattern minAlt = Pattern.compile("(?:min|min\\.)\\s*(?:rs|rs\\.)?\\s*(\\d+)");
            Matcher minAltM = minAlt.matcher(textLower);
            if (minAltM.find()) {
                try {
                    offer.minTransactionAmount = Double.parseDouble(minAltM.group(1));
                } catch (Exception ignored) {}
            }
        }

        // 5. User Eligibility
        if (textLower.contains("last 60 days") || textLower.contains("60 days")) {
            offer.userEligibility = "INACTIVE_60_DAYS";
        } else if (textLower.contains("first time") || textLower.contains("new user")) {
            offer.userEligibility = "NEW_USER";
        }

        // 6. Title
        if (offer.discountType.equals("UPTO_CASHBACK")) {
            offer.title = offer.provider + " Up to ₹" + (int) offer.cashbackAmount + " Cashback";
        } else if (offer.cashbackAmount > 0) {
            offer.title = offer.provider + " Assured ₹" + (int) offer.cashbackAmount + " Cashback";
        } else if (offer.discountAmount > 0) {
            offer.title = offer.provider + " ₹" + (int) offer.discountAmount + " Instant Discount";
        } else {
            offer.title = offer.provider + " Promotional Offer";
        }

        return offer;
    }
}
