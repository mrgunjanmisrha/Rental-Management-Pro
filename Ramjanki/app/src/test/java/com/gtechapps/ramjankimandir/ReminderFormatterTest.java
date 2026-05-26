package com.gtechapps.ramjankimandir;

import static org.junit.Assert.assertTrue;

import com.gtechapps.ramjankimandir.util.ReminderFormatter;

import org.junit.Test;

public class ReminderFormatterTest {

    @Test
    public void buildReminder_containsAllPendingAmounts() {
        String reminder = ReminderFormatter.buildReminder(
                "Ramesh",
                "Room 101",
                "2026-05",
                5000D,
                450D,
                100D,
                "Please pay by Sunday."
        );

        assertTrue(reminder.contains("Ramesh"));
        assertTrue(reminder.contains("Room 101"));
        assertTrue(reminder.contains("Rs 5,000"));
        assertTrue(reminder.contains("Rs 450"));
        assertTrue(reminder.contains("Rs 100"));
        assertTrue(reminder.contains("Please pay by Sunday."));
    }
}
