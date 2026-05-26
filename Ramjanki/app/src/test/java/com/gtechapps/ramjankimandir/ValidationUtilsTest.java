package com.gtechapps.ramjankimandir;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.gtechapps.ramjankimandir.util.ValidationUtils;

import org.junit.Test;

public class ValidationUtilsTest {

    @Test
    public void normalizeDate_returnsSameDateForValidInput() {
        assertEquals("2026-05-13", ValidationUtils.normalizeDate("2026-05-13", "Entry date"));
    }

    @Test
    public void normalizeDate_rejectsInvalidCalendarDate() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> ValidationUtils.normalizeDate("2026-02-30", "Entry date")
        );

        assertEquals("Entry date must be in YYYY-MM-DD format.", error.getMessage());
    }

    @Test
    public void requireNonNegative_rejectsNegativeNumbers() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> ValidationUtils.requireNonNegative(-1D, "Charge")
        );

        assertEquals("Charge cannot be negative.", error.getMessage());
    }
}
