package com.gtechapps.ramjankimandir.util;

import com.gtechapps.ramjankimandir.model.BillingStatus;
import com.gtechapps.ramjankimandir.model.GaragePayment;
import com.gtechapps.ramjankimandir.model.RoomPayment;

public final class BillingRules {

    private BillingRules() {
    }

    public static boolean isFinalLocked(String status) {
        return BillingStatus.COMPLETE.equals(status) || BillingStatus.TRANSFER.equals(status);
    }

    public static void syncRoomDerivedStatuses(RoomPayment payment) {
        if (payment == null) {
            return;
        }
        payment.rent_status = payment.rent_received ? BillingStatus.RECEIVED : BillingStatus.PENDING;
        if (payment.bijlibill_received) {
            payment.bijlibill_status = BillingStatus.RECEIVED;
            return;
        }
        if (ValidationUtils.isBlank(payment.bill_current_unit)) {
            payment.bijlibill_status = BillingStatus.NOT_CALCULATED;
        } else {
            payment.bijlibill_status = BillingStatus.CALCULATED;
        }
    }

    public static void validateRoomPaymentUpdate(RoomPayment existing, RoomPayment updated) {
        if (existing != null && isFinalLocked(existing.status)) {
            throw new IllegalStateException("This room monthly record is locked after final status save.");
        }
        if (updated == null) {
            throw new IllegalArgumentException("Room payment is missing.");
        }
        requireFinalStatus(updated.status);
        ValidationUtils.requireNonNegative(updated.room_rent, "Room rent");
        ValidationUtils.requireNonNegative(updated.bijlibill, "Bijli bill");
        ValidationUtils.requireNonNegative(updated.bijli_charge, "Bijli charge");
        ValidationUtils.requireNonNegative(updated.bill_start_unit, "Bill start unit");
        ValidationUtils.requireNonNegative(updated.other_charge, "Other charge");
        if (updated.rent_received && !BillingStatus.RECEIVED.equals(updated.rent_status)) {
            throw new IllegalArgumentException("Rent status must be received when rent is marked received.");
        }
        if (updated.bijlibill_received && !BillingStatus.RECEIVED.equals(updated.bijlibill_status)) {
            throw new IllegalArgumentException("Bijli bill status must be received when bijli bill is marked received.");
        }
        if (!ValidationUtils.isBlank(updated.bill_current_unit)) {
            double currentUnit = parseCurrentUnit(updated.bill_current_unit);
            if (currentUnit < updated.bill_start_unit) {
                throw new IllegalArgumentException("Current unit cannot be less than start unit.");
            }
        }
        if (updated.bijlibill_received && ValidationUtils.isBlank(updated.bill_current_unit)) {
            throw new IllegalArgumentException("Calculate bijli bill before marking it received.");
        }
        if (BillingStatus.COMPLETE.equals(updated.status)) {
            if (ValidationUtils.isBlank(updated.bill_current_unit)) {
                throw new IllegalArgumentException("Current unit is required before completing the room bill.");
            }
            if (!updated.rent_received || !updated.bijlibill_received) {
                throw new IllegalArgumentException("Complete status requires both room rent and bijli bill to be received.");
            }
        }
        if (BillingStatus.TRANSFER.equals(updated.status) && ValidationUtils.isBlank(updated.bill_current_unit)) {
            throw new IllegalArgumentException("Current unit is required before transferring the room bill.");
        }
    }

    public static void validateGaragePaymentUpdate(GaragePayment existing, GaragePayment updated) {
        if (existing != null && isFinalLocked(existing.status)) {
            throw new IllegalStateException("This garage monthly record is locked after final status save.");
        }
        if (updated == null) {
            throw new IllegalArgumentException("Garage payment is missing.");
        }
        requireFinalStatus(updated.status);
        ValidationUtils.requireNonNegative(updated.charge, "Garage charge");
        if (BillingStatus.COMPLETE.equals(updated.status) && !updated.charge_received) {
            throw new IllegalArgumentException("Complete status requires garage charge to be received.");
        }
    }

    private static void requireFinalStatus(String status) {
        if (!BillingStatus.PENDING.equals(status)
                && !BillingStatus.COMPLETE.equals(status)
                && !BillingStatus.TRANSFER.equals(status)) {
            throw new IllegalArgumentException("Invalid final status.");
        }
    }

    private static double parseCurrentUnit(String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Current unit must be numeric.");
        }
    }
}
