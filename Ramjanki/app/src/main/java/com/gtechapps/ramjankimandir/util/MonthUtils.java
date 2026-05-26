package com.gtechapps.ramjankimandir.util;

import com.gtechapps.ramjankimandir.model.BillingMonth;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class MonthUtils {

    private static final SimpleDateFormat MONTH_ID_FORMAT = new SimpleDateFormat("yyyy-MM", Locale.US);
    private static final SimpleDateFormat MONTH_NAME_FORMAT = new SimpleDateFormat("MMMM yyyy", Locale.US);

    private MonthUtils() {
    }

    public static String currentMonthId() {
        return MONTH_ID_FORMAT.format(new Date());
    }

    public static BillingMonth buildMonth(String monthId) {
        BillingMonth month = new BillingMonth();
        month.month_id = monthId;
        month.month_name = displayName(monthId);
        month.createAt = TimeUtils.nowIsoTimestamp();
        return month;
    }

    public static BillingMonth nextMonth(List<BillingMonth> months) {
        if (months == null || months.isEmpty()) {
            return buildMonth(currentMonthId());
        }
        List<BillingMonth> sorted = sortMonths(months);
        String latest = sorted.get(sorted.size() - 1).month_id;
        Calendar calendar = Calendar.getInstance();
        try {
            Date parsed = MONTH_ID_FORMAT.parse(latest);
            if (parsed != null) {
                calendar.setTime(parsed);
            }
        } catch (ParseException ignored) {
        }
        calendar.add(Calendar.MONTH, 1);
        return buildMonth(MONTH_ID_FORMAT.format(calendar.getTime()));
    }

    public static List<BillingMonth> sortMonths(List<BillingMonth> months) {
        List<BillingMonth> sorted = new ArrayList<>(months);
        Collections.sort(sorted, Comparator.comparing(month -> month.month_id));
        return sorted;
    }

    public static String displayName(String monthId) {
        try {
            Date parsed = MONTH_ID_FORMAT.parse(monthId);
            return parsed == null ? monthId : MONTH_NAME_FORMAT.format(parsed);
        } catch (ParseException exception) {
            return monthId;
        }
    }
}
