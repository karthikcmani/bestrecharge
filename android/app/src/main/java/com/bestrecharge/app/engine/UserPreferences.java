package com.bestrecharge.app.engine;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class UserPreferences implements Serializable {
    public List<String> installedPaymentApps = new ArrayList<>();
    public boolean isPaytmNewOrInactive60d = true;
    public boolean isBhimNewUser = true;
    public boolean isPhonePeNewUser = false;
    public boolean hasRupayCreditCard = false;

    public UserPreferences() {
        installedPaymentApps.add("PAYTM");
        installedPaymentApps.add("BHIM");
        installedPaymentApps.add("PHONEPE");
        installedPaymentApps.add("GPAY");
    }
}
