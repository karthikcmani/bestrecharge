package com.bestrecharge.app.ui.ocr;

import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bestrecharge.app.data.local.entities.OfferEntity;
import com.bestrecharge.app.data.repository.RechargeRepository;
import com.bestrecharge.app.databinding.ActivityScreenshotImportBinding;
import com.bestrecharge.app.ocr.ParsedOffer;
import com.bestrecharge.app.ocr.ScreenshotOcrManager;

import java.util.UUID;

public class ScreenshotImportActivity extends AppCompatActivity {

    private ActivityScreenshotImportBinding binding;
    private ParsedOffer currentParsed;
    private ActivityResultLauncher<String> galleryLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityScreenshotImportBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnOcrBack.setOnClickListener(v -> finish());

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                this::handleImageUri
        );

        binding.btnPickGallery.setOnClickListener(v -> galleryLauncher.launch("image/*"));

        // Quick test button simulating screenshot terms from Vi app screenshot!
        binding.btnLoadSampleOcr.setOnClickListener(v -> loadSampleViPaytmOcr());

        binding.btnConfirmSaveOffer.setOnClickListener(v -> saveExtractedOffer());
    }

    private void handleImageUri(Uri uri) {
        if (uri == null) return;
        try {
            Bitmap bitmap;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(getContentResolver(), uri));
            } else {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            }
            binding.ivScreenshotPreview.setImageBitmap(bitmap);

            // Execute local ML Kit OCR
            ScreenshotOcrManager.processScreenshot(bitmap, new ScreenshotOcrManager.OcrCallback() {
                @Override
                public void onSuccess(ParsedOffer parsedOffer, String rawText) {
                    currentParsed = parsedOffer;
                    populateForm(parsedOffer);
                    Toast.makeText(ScreenshotImportActivity.this, "Screenshot processed via ML Kit!", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(ScreenshotImportActivity.this, "OCR failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "Failed to load image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void loadSampleViPaytmOcr() {
        String sampleText = "terms & conditions\n" +
                "- Offer valid from 1st to 30th November 2026\n" +
                "- Min assured cashback of ₹125\n" +
                "- Applicable on min transaction value of ₹179\n" +
                "- Eligibility for Cashback: To avail the offer, the user must complete a payment on the merchant app/website using Paytm UPI\n" +
                "- Offer valid for Users who have not used Paytm anywhere in the last 60 days; will get flat ₹125 cashback + Gold Coins on every payment.\n" +
                "- On the Vi app the user has to select Paytm UPI to complete the payment.";

        ParsedOffer parsed = ScreenshotOcrManager.parseExtractedText(sampleText);
        currentParsed = parsed;
        populateForm(parsed);
        Toast.makeText(this, "Loaded sample OCR terms into review form!", Toast.LENGTH_SHORT).show();
    }

    private void populateForm(ParsedOffer offer) {
        binding.etExtractedTitle.setText(offer.title);
        binding.etExtractedProvider.setText(offer.provider);
        binding.etExtractedCashback.setText(String.valueOf((int) (offer.cashbackAmount > 0 ? offer.cashbackAmount : offer.discountAmount)));
        binding.etExtractedMinTxn.setText(String.valueOf((int) offer.minTransactionAmount));
        binding.etExtractedEligibility.setText(offer.userEligibility);
        binding.etExtractedTerms.setText(offer.termsAndConditions);
    }

    private void saveExtractedOffer() {
        String title = binding.etExtractedTitle.getText().toString().trim();
        String provider = binding.etExtractedProvider.getText().toString().trim();
        if (title.isEmpty()) title = "User Imported Offer";
        if (provider.isEmpty()) provider = "UPI";

        double cashback = 0;
        try {
            cashback = Double.parseDouble(binding.etExtractedCashback.getText().toString());
        } catch (Exception ignored) {}

        double minTxn = 0;
        try {
            minTxn = Double.parseDouble(binding.etExtractedMinTxn.getText().toString());
        } catch (Exception ignored) {}

        String eligibility = binding.etExtractedEligibility.getText().toString().trim();
        String terms = binding.etExtractedTerms.getText().toString().trim();

        OfferEntity entity = new OfferEntity(
                "imported_" + UUID.randomUUID().toString().substring(0, 8),
                title,
                provider,
                currentParsed != null ? currentParsed.category : "UPI",
                "User confirmed offer imported via OCR",
                currentParsed != null ? currentParsed.discountType : "FLAT_CASHBACK",
                0.0,
                cashback,
                minTxn,
                cashback,
                eligibility.isEmpty() ? "ALL" : eligibility,
                provider + " UPI",
                "ALL",
                true,
                "2026-11-30",
                terms,
                "USER_IMPORTED",
                "NONE",
                true
        );

        new RechargeRepository(getApplication()).insertOffer(entity);
        Toast.makeText(this, "Offer successfully saved to local intelligence!", Toast.LENGTH_SHORT).show();
        finish();
    }
}
