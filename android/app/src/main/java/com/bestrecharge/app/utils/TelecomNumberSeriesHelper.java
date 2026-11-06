package com.bestrecharge.app.utils;

/**
 * TelecomNumberSeriesHelper provides Indian National Numbering Plan (DoT)
 * operator and telecom circle allocation detection for 10-digit mobile numbers.
 */
public class TelecomNumberSeriesHelper {

    public static class AllocationInfo {
        public final String operatorCode;
        public final String operatorName;
        public final String circleName;
        public final String circleCode;

        public AllocationInfo(String operatorCode, String operatorName, String circleName, String circleCode) {
            this.operatorCode = operatorCode;
            this.operatorName = operatorName;
            this.circleName = circleName;
            this.circleCode = circleCode;
        }
    }

    public static AllocationInfo getAllocation(String rawNumber) {
        if (rawNumber == null) return null;
        String digits = rawNumber.replaceAll("[^0-9]", "");
        if (digits.length() > 10) {
            digits = digits.substring(digits.length() - 10);
        }
        if (digits.length() < 4) return null;

        String p4 = digits.substring(0, 4);
        String p3 = digits.substring(0, 3);

        // 1. Check Kerala Series (e.g. 98478 37607 / 98478 37666)
        if (p4.equals("9847") || p4.equals("9846") || p4.equals("9744") || p4.equals("9745") ||
                p4.equals("9746") || p4.equals("9747") || p4.equals("9946") || p4.equals("9947") ||
                p4.equals("9995") || p4.equals("9048")) {
            return new AllocationInfo("VI", "Vodafone Idea (Vi)", "Kerala", "KL");
        }

        // BSNL Kerala
        if (p4.equals("9446") || p4.equals("9447") || p4.equals("9495") || p4.equals("9496") || p4.equals("9497")) {
            return new AllocationInfo("BSNL", "BSNL", "Kerala", "KL");
        }

        // 2. Check Delhi NCR Series
        if (p4.equals("9811") || p4.equals("9871") || p4.equals("9891") || p4.equals("9911") ||
                p4.equals("9971") || p4.equals("9711") || p4.equals("9818")) {
            return new AllocationInfo("VI", "Vodafone Idea (Vi)", "Delhi NCR", "DL");
        }
        if (p4.equals("9810") || p4.equals("9910") || p4.equals("9873") || p4.equals("9958")) {
            return new AllocationInfo("AIRTEL", "Bharti Airtel", "Delhi NCR", "DL");
        }

        // 3. Check Mumbai & Maharashtra
        if (p4.equals("9820") || p4.equals("9821") || p4.equals("9833") || p4.equals("9920") || p4.equals("9892")) {
            return new AllocationInfo("VI", "Vodafone Idea (Vi)", "Mumbai", "MU");
        }
        if (p4.equals("9822") || p4.equals("9823") || p4.equals("9922")) {
            return new AllocationInfo("VI", "Vodafone Idea (Vi)", "Maharashtra & Goa", "MH");
        }
        if (p4.equals("9890") || p4.equals("9960") || p4.equals("9860")) {
            return new AllocationInfo("AIRTEL", "Bharti Airtel", "Maharashtra & Goa", "MH");
        }

        // 4. Check Karnataka
        if (p4.equals("9844") || p4.equals("9886") || p4.equals("9900") || p4.equals("9945")) {
            return new AllocationInfo("VI", "Vodafone Idea (Vi)", "Karnataka", "KA");
        }
        if (p4.equals("9845") || p4.equals("9880") || p4.equals("9902") || p4.equals("9980")) {
            return new AllocationInfo("AIRTEL", "Bharti Airtel", "Karnataka", "KA");
        }

        // 5. Check Tamil Nadu & Chennai
        if (p4.equals("9840") || p4.equals("9841") || p4.equals("9884") || p4.equals("9940") || p4.equals("9843")) {
            return new AllocationInfo("VI", "Vodafone Idea (Vi)", "Tamil Nadu", "TN");
        }

        // 6. Check Gujarat
        if (p4.equals("9824") || p4.equals("9825") || p4.equals("9898") || p4.equals("9925")) {
            return new AllocationInfo("VI", "Vodafone Idea (Vi)", "Gujarat", "GJ");
        }

        // 7. Check Jio Series (Starts with 6, 7, 8 in India)
        if (p3.equals("700") || p3.equals("701") || p3.equals("702") || p3.equals("703") ||
                p3.equals("704") || p3.equals("705") || p3.equals("706") || p3.equals("707") ||
                p3.equals("708") || p3.equals("709") || p3.equals("790") || p3.equals("797") ||
                p3.equals("798") || p3.equals("799") || p3.equals("620") || p3.equals("628") ||
                p3.equals("629") || p3.equals("630") || p3.equals("638") || p3.equals("639") ||
                p3.equals("808") || p3.equals("809") || p3.equals("810") || p3.equals("890") ||
                p3.equals("891") || p3.equals("892") || p3.equals("893")) {
            return new AllocationInfo("JIO", "Reliance Jio", "Delhi NCR", "DL");
        }

        // 8. General BSNL
        if (p3.equals("944") || p3.equals("941") || p3.equals("942") || p3.equals("943") || p3.equals("945")) {
            return new AllocationInfo("BSNL", "BSNL", "Pan India", "IN");
        }

        // Default fallback if unknown
        return null;
    }
}
