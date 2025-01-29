package com.whatstheplan.events.utils;

import com.whatstheplan.events.model.request.EventRequest;
import lombok.experimental.UtilityClass;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.WeekDay;

import java.time.LocalDateTime;
import java.time.temporal.Temporal;
import java.util.List;

import static net.fortuna.ical4j.transform.recurrence.Frequency.DAILY;
import static net.fortuna.ical4j.transform.recurrence.Frequency.MONTHLY;
import static net.fortuna.ical4j.transform.recurrence.Frequency.WEEKLY;

@UtilityClass
public class RecurrenceUtils {

    public static String generateRRule(EventRequest.RecurrenceRequest recurrence) {
        Recur.Builder<Temporal> builder = new Recur.Builder<>();

        // Frequency
        switch (recurrence.getFrequency()) {
            case "DAILY" -> builder.frequency(DAILY);
            case "WEEKLY" -> builder.frequency(WEEKLY);
            case "MONTHLY" -> builder.frequency(MONTHLY);
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
        }

        // Handle Monthly (byMonthDay)
        if (recurrence.getByMonthDay() != null) {
            builder.monthDayList(recurrence.getByMonthDay());
        }

        // Set End Conditions
        if (recurrence.getUntil() != null) {
            builder.until(LocalDateTime.parse(recurrence.getUntil().toString()));
        } else if (recurrence.getCount() != null) {
            builder.count(recurrence.getCount());
        }

        return builder.build().toString();
    }
}
