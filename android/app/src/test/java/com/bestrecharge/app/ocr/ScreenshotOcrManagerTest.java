package com.bestrecharge.app.ocr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ScreenshotOcrManagerTest {

    @Test
    public void testParseViPaytmScreenshotText() {
        String ocrSample = "terms & conditions\n" +
                "- Offer valid from 1st to 30th November 2026\n" +
                "- Min assured cashback of ₹125\n" +
                "- Applicable on min transaction value of ₹179\n" +
                "- Eligibility for Cashback: To avail the offer, the user must complete a payment on the merchant app/website using Paytm UPI\n" +
                "- Offer valid for Users who have not used Paytm anywhere in the last 60 days; will get flat ₹125 cashback + Gold Coins on every payment.";

        ParsedOffer parsed = ScreenshotOcrManager.parseExtractedText(ocrSample);

        assertNotNull(parsed);
        assertEquals("Paytm", parsed.provider);
        assertEquals(125.0, parsed.cashbackAmount, 0.01);
        assertEquals(179.0, parsed.minTransactionAmount, 0.01);
        assertEquals("INACTIVE_60_DAYS", parsed.userEligibility);
        assertTrue(parsed.title.contains("Paytm"));
        assertTrue(parsed.title.contains("125"));
    }

    @Test
    public void testParseBhimScreenshotText() {
        String ocrSample = "terms & conditions\n" +
                "- Offer valid from 1st to 30th November 2026\n" +
                "- Offer applicable on payments using BHIM Payments App\n" +
                "- Applicable on min transaction value of ₹99\n" +
                "- get assured cashback upto Rs.75";

        ParsedOffer parsed = ScreenshotOcrManager.parseExtractedText(ocrSample);

        assertNotNull(parsed);
        assertEquals("BHIM", parsed.provider);
        assertEquals(75.0, parsed.cashbackAmount, 0.01);
        assertEquals(99.0, parsed.minTransactionAmount, 0.01);
        assertEquals("UPTO_CASHBACK", parsed.discountType);
    }
}
