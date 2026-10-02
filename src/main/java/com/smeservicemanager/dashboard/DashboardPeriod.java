package com.smeservicemanager.dashboard;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

public enum DashboardPeriod {
    DAILY, MONTHLY, YEARLY;

    public record Window(String key, LocalDate start, LocalDate end) {}

    public Window window(String key, LocalDate date) {
        LocalDate start = switch (this) {
            case DAILY -> date;
            case MONTHLY -> date.withDayOfMonth(1);
            case YEARLY -> date.with(TemporalAdjusters.firstDayOfYear());
        };
        return new Window(key, start, shift(start, 1));
    }

    public LocalDate shift(LocalDate date, int amount) {
        return switch (this) {
            case DAILY -> date.plusDays(amount);
            case MONTHLY -> date.withDayOfMonth(1).plusMonths(amount);
            case YEARLY -> date.withDayOfYear(1).plusYears(amount);
        };
    }

    public List<Window> windows(LocalDate date) {
        Window selected = window("selected", date);
        var windows = new ArrayList<Window>();
        windows.add(selected);
        windows.add(window("previous", shift(selected.start(), -1)));
        LocalDate cursor = this == DAILY ? date.minusDays(6) : selected.start();
        for (int i = 0; cursor.isBefore(selected.end()); i++) {
            LocalDate next = this == YEARLY ? cursor.plusMonths(1) : cursor.plusDays(1);
            windows.add(new Window("trend" + i, cursor, next));
            cursor = next;
        }
        return windows;
    }

    public static LocalDateTime cutoff(Window window, LocalDateTime now) {
        return window.end().atStartOfDay().isBefore(now) ? window.end().atStartOfDay() : now;
    }
}
