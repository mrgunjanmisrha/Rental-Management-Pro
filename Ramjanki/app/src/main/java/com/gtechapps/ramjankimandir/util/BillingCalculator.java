package com.gtechapps.ramjankimandir.util;

import com.gtechapps.ramjankimandir.model.BillingStatus;

public final class BillingCalculator {

    private BillingCalculator() {
    }

    public static double calculateElectricityAmount(double startUnit, double endUnit, double unitRate, double fixedCharge) {
        double consumed = Math.max(0D, endUnit - startUnit);
        return (consumed * unitRate) + fixedCharge;
    }

    public static double calculatePendingAmount(double totalAmount, double receivedAmount) {
        return Math.max(0D, totalAmount - receivedAmount);
    }

    public static String resolveStatus(double totalAmount, double receivedAmount, boolean pastGracePeriod) {
        if (receivedAmount >= totalAmount) {
            return BillingStatus.COMPLETE;
        }
        if (pastGracePeriod) {
            return BillingStatus.OVERDUE;
        }
        return BillingStatus.PENDING;
    }

    public static double calculateRoomTotal(double roomRent, double electricityBill, double otherCharge) {
        return Math.max(0D, roomRent + electricityBill + otherCharge);
    }
}
