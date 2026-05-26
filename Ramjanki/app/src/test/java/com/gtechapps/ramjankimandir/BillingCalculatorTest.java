package com.gtechapps.ramjankimandir;

import static org.junit.Assert.assertEquals;

import com.gtechapps.ramjankimandir.model.BillingStatus;
import com.gtechapps.ramjankimandir.util.BillingCalculator;

import org.junit.Test;

public class BillingCalculatorTest {

    @Test
    public void calculateElectricityAmount_includesFixedCharge() {
        double total = BillingCalculator.calculateElectricityAmount(120D, 155D, 8.5D, 75D);
        assertEquals(372.5D, total, 0.01D);
    }

    @Test
    public void calculatePendingAmount_neverReturnsNegativeValue() {
        double pending = BillingCalculator.calculatePendingAmount(5000D, 5200D);
        assertEquals(0D, pending, 0.0D);
    }

    @Test
    public void resolveStatus_returnsOverdueAfterGracePeriod() {
        String status = BillingCalculator.resolveStatus(5000D, 1500D, true);
        assertEquals(BillingStatus.OVERDUE, status);
    }
}
