package com.gtechapps.ramjankimandir.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

public final class ValidationUtils {

    private static final String DATE_PATTERN = "yyyy-MM-dd";

    private ValidationUtils() {
    }

    public static String normalizeDate(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        SimpleDateFormat format = new SimpleDateFormat(DATE_PATTERN, Locale.US);
        format.setLenient(false);
        try {
            return format.format(format.parse(value.trim()));
        } catch (ParseException exception) {
            throw new IllegalArgumentException(label + " must be in YYYY-MM-DD format.");
        }
    }

    public static double requireNonNegative(double value, String label) {
        if (value < 0D) {
            throw new IllegalArgumentException(label + " cannot be negative.");
        }
        return value;
    }

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
