package com.gtechapps.ramjankimandir.util;

import java.text.DecimalFormat;

public final class ReminderFormatter {

    private static final DecimalFormat RUPEE_FORMAT = new DecimalFormat("#,##0");

    private ReminderFormatter() {
    }

    public static String buildReminder(String recipientName,
                                       String unitLabel,
                                       String monthId,
                                       double rentPending,
                                       double electricityPending,
                                       double lateFee,
                                       String note) {
        StringBuilder builder = new StringBuilder();
        builder.append("Namaste ").append(recipientName).append(",\n\n");
        builder.append("This is a payment reminder for ").append(unitLabel)
                .append(" for ").append(monthId).append(".\n");
        builder.append("Pending room/garage rent: Rs ").append(RUPEE_FORMAT.format(rentPending)).append("\n");
        builder.append("Pending electricity: Rs ").append(RUPEE_FORMAT.format(electricityPending)).append("\n");
        builder.append("Late fee: Rs ").append(RUPEE_FORMAT.format(lateFee)).append("\n");
        if (note != null && !note.trim().isEmpty()) {
            builder.append(note).append("\n");
        }
        builder.append("\nPlease clear the dues at the earliest. Thank you.");
        return builder.toString();
    }
}
