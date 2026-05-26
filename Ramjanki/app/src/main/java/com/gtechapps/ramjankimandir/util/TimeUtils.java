package com.gtechapps.ramjankimandir.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class TimeUtils {

    private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);

    private TimeUtils() {
    }

    public static String nowIsoTimestamp() {
        return TIMESTAMP_FORMAT.format(new Date());
    }
}
