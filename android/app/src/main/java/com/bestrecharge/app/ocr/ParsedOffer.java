package com.bestrecharge.app.ocr;

import java.io.Serializable;

public class ParsedOffer implements Serializable {
    public String title;
    public String provider;
    public String category;
    public String discountType;
    public double discountAmount;
    public double cashbackAmount;
    public double minTransactionAmount;
    public double maxCashback;
    public String userEligibility;
    public String paymentMethodRequired;
    public String expiryDate;
    public String termsAndConditions;
    public String verificationStatus;
    public int confidenceScore;

    public ParsedOffer() {
        this.provider = "UPI";
        this.category = "UPI";
        this.discountType = "FLAT_CASHBACK";
        this.userEligibility = "ALL";
        this.paymentMethodRequired = "ANY";
        this.verificationStatus = "USER_CONFIRMATION_REQUIRED";
        this.confidenceScore = 85;
    }
}
