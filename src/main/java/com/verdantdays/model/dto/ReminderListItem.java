package com.verdantdays.model.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReminderListItem(
        int id,
        String name,
        String phoneNumber,
        String reminderType,
        LocalDate lastOrderDate,
        LocalDateTime lastReminderSentAt
) {}
