package com.whatstheplan.events.utils;

import com.whatstheplan.events.model.Recurrence;
import lombok.experimental.UtilityClass;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.WeekDay;
import net.fortuna.ical4j.transform.recurrence.Frequency;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static net.fortuna.ical4j.transform.recurrence.Frequency.DAILY;
import static net.fortuna.ical4j.transform.recurrence.Frequency.MONTHLY;
import static net.fortuna.ical4j.transform.recurrence.Frequency.WEEKLY;
import static net.fortuna.ical4j.transform.recurrence.Frequency.YEARLY;

@UtilityClass
public class RecurrenceUtils {

    public static String generateRRule(Recurrence recurrence) {
        if (recurrence == null) {
            return null;
        }

        Recur.Builder<LocalDateTime> builder = new Recur.Builder<>();

        // Frequency
        switch (recurrence.getFrequency()) {
            case "DAILY" -> builder.frequency(DAILY);
            case "WEEKLY" -> builder.frequency(WEEKLY);
            case "MONTHLY" -> builder.frequency(MONTHLY);
            case "YEARLY" -> builder.frequency(YEARLY);
            default -> {
                return null;
            }
        }

        // Interval
        if (recurrence.getInterval() != null && recurrence.getInterval() > 1) {
            builder.interval(recurrence.getInterval());
        }

        // Handle Weekly (byDays)
        if (recurrence.getByDays() != null) {

            List<WeekDay> list = recurrence.getByDays().stream()
                    .map(day -> new WeekDay(day.substring(0, 2))).toList();
            builder.dayList(list);
        } else {
            builder.dayList(Collections.emptyList());
        }

        // Handle Monthly (byMonthDay)
        if (recurrence.getByMonthDay() != null) {
            builder.monthDayList(recurrence.getByMonthDay());
        } else {
            builder.monthDayList(Collections.emptyList());
        }

        // Set End Conditions
        if (recurrence.getUntil() != null) {
            builder.until(LocalDateTime.parse(recurrence.getUntil().toString()));
        } else if (recurrence.getCount() != null) {
            builder.count(recurrence.getCount());
        }

        return builder.build().toString();
    }


    public static Recurrence parseRRule(String rule) {
        if (rule == null) {
            return null;
        }

        Recur<LocalDateTime> recur = new Recur<>(rule);
        return Recurrence.builder()
                .frequency(Optional.ofNullable(recur.getFrequency()).map(Frequency::name).orElse(null))
                .interval(recur.getInterval() != -1 ? recur.getInterval() : null)
                .byDays(Optional.ofNullable(recur.getDayList())
                        .orElse(Collections.emptyList())
                        .stream()
                        .map(WeekDay::getDay)
                        .map(WeekDay.Day::name)
                        .toList())
                .byMonthDay(Optional.ofNullable(recur.getMonthDayList()).orElse(Collections.emptyList()))
                .until(recur.getUntil() != null ? LocalDateTime.from(recur.getUntil()) : null)
                .count(recur.getCount() != -1 ? recur.getCount() : null)
                .build();
    }
}
