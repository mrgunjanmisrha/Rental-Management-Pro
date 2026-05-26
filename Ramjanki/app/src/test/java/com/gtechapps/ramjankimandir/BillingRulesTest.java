package com.gtechapps.ramjankimandir;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.gtechapps.ramjankimandir.model.BillingStatus;
import com.gtechapps.ramjankimandir.model.GaragePayment;
import com.gtechapps.ramjankimandir.model.RoomPayment;
import com.gtechapps.ramjankimandir.util.BillingRules;

import org.junit.Test;

public class BillingRulesTest {

    @Test
    public void syncRoomDerivedStatuses_marksCalculatedWhenUnitPresent() {
        RoomPayment payment = new RoomPayment();
        payment.bill_current_unit = "145";
        BillingRules.syncRoomDerivedStatuses(payment);

        assertEquals(BillingStatus.CALCULATED, payment.bijlibill_status);
        assertEquals(BillingStatus.PENDING, payment.rent_status);
    }

    @Test
    public void validateRoomPaymentUpdate_rejectsCompleteWithoutReceipts() {
        RoomPayment existing = new RoomPayment();
        existing.status = BillingStatus.PENDING;

        RoomPayment updated = new RoomPayment();
        updated.status = BillingStatus.COMPLETE;
        updated.bill_start_unit = 120D;
        updated.bill_current_unit = "140";
        updated.room_rent = 4000D;
        updated.bijli_charge = 8D;

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> BillingRules.validateRoomPaymentUpdate(existing, updated)
        );

        assertEquals("Complete status requires both room rent and bijli bill to be received.", error.getMessage());
    }

    @Test
    public void validateRoomPaymentUpdate_rejectsAlreadyLockedRecord() {
        RoomPayment existing = new RoomPayment();
        existing.status = BillingStatus.COMPLETE;

        RoomPayment updated = new RoomPayment();
        updated.status = BillingStatus.COMPLETE;
        updated.bill_start_unit = 100D;
        updated.bill_current_unit = "100";

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> BillingRules.validateRoomPaymentUpdate(existing, updated)
        );

        assertEquals("This room monthly record is locked after final status save.", error.getMessage());
    }

    @Test
    public void validateGaragePaymentUpdate_requiresChargeReceivedForComplete() {
        GaragePayment existing = new GaragePayment();
        existing.status = BillingStatus.PENDING;

        GaragePayment updated = new GaragePayment();
        updated.status = BillingStatus.COMPLETE;
        updated.charge = 1500D;
        updated.charge_received = false;

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> BillingRules.validateGaragePaymentUpdate(existing, updated)
        );

        assertEquals("Complete status requires garage charge to be received.", error.getMessage());
    }
}
