package com.whatstheplan.events.utils;

import com.whatstheplan.events.model.Recurrence;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class RecurrenceUtilsTest {

    @ParameterizedTest
    @MethodSource("provideValidRecurrenceRequests")
    void generateRRule_ValidCases_ReturnsCorrectRRule(Recurrence recurrence, String expectedRrule) {
        String actualRrule = RecurrenceUtils.generateRRule(recurrence);
        assertEquals(expectedRrule, actualRrule);
    }

    @ParameterizedTest
    @MethodSource("provideInvalidFrequencyCases")
    void generateRRule_InvalidFrequency_ReturnsNull(Recurrence recurrence) {
        assertNull(RecurrenceUtils.generateRRule(recurrence));
    }

    @ParameterizedTest
    @MethodSource("provideInvalidByDaysCases")
    void generateRRule_InvalidByDays_ThrowsException(Recurrence recurrence) {
        assertThrows(StringIndexOutOfBoundsException.class, () -> RecurrenceUtils.generateRRule(recurrence));
    }

    @ParameterizedTest
    @MethodSource("provideIntervalEdgeCases")
    void generateRRule_IntervalEdgeCases_OmitsInterval(Recurrence recurrence, String expectedRrule) {
        String actualRrule = RecurrenceUtils.generateRRule(recurrence);
        assertEquals(expectedRrule, actualRrule);
    }

    @ParameterizedTest
    @MethodSource("provideRRuleStrings")
    void testParseRRule(String rrule, Recurrence expected) {
        Recurrence actual = RecurrenceUtils.parseRRule(rrule);

        assertRecurrenceEquals(expected, actual);
    }

    public static void assertRecurrenceEquals(Recurrence expected, Recurrence actual) {
        assertNotNull(actual);
        assertThat(actual.getFrequency())
                .isEqualTo(expected.getFrequency());
        assertThat(actual.getInterval())
                .matches(interval ->
                        ((expected.getInterval() == null || expected.getInterval() <= 1)
                                && interval == null)
                                || (interval.equals(expected.getInterval())));
        assertThat(actual.getByDays())
                .matches(days ->
                        (expected.getByDays() == null && Collections.emptyList().equals(days)
                                || (days.equals(expected.getByDays()))));
        assertThat(actual.getByMonthDay())
                .matches(days ->
                        (expected.getByMonthDay() == null && Collections.emptyList().equals(days)
                                || (days.equals(expected.getByMonthDay()))));
        assertThat(actual)
                .satisfies(rec -> {
                    if (!(rec.getUntil() == null && expected.getUntil() == null)) {
                        assertThat(rec.getUntil())
                                .isEqualToIgnoringNanos(expected.getUntil());
                    }
                });
        assertThat(actual.getCount())
                .isEqualTo(expected.getCount());
    }

    private static Recurrence createRecurrenceRequest(
            String frequency, Integer interval, List<String> byDays, List<Integer> byMonthDay, LocalDateTime until, Integer count) {
        return Recurrence.builder()
                .frequency(frequency)
                .interval(interval)
                .byDays(byDays)
                .byMonthDay(byMonthDay)
                .until(until)
                .count(count)
                .build();
    }

    private static Stream<Arguments> provideValidRecurrenceRequests() {
        return Stream.of(
                // DAILY frequency
                Arguments.of(
                        createRecurrenceRequest("DAILY", 2, null, null, null, 3),
                        "FREQ=DAILY;COUNT=3;INTERVAL=2"
                ),
                Arguments.of(
                        createRecurrenceRequest("DAILY", 1, null, null, LocalDateTime.of(2023, 10, 5, 10, 0), null),
                        "FREQ=DAILY;UNTIL=20231005T100000"
                ),
                // WEEKLY frequency
                Arguments.of(
                        createRecurrenceRequest("WEEKLY", 3, List.of("MO", "WE"), null, null, 5),
                        "FREQ=WEEKLY;COUNT=5;INTERVAL=3;BYDAY=MO,WE"
                ),
                Arguments.of(
                        createRecurrenceRequest("WEEKLY", 1, List.of("FR"), null, null, null),
                        "FREQ=WEEKLY;BYDAY=FR"
                ),
                Arguments.of(
                        createRecurrenceRequest("WEEKLY", null, Collections.emptyList(), null, null, null),
                        "FREQ=WEEKLY"
                ),
                // MONTHLY frequency
                Arguments.of(
                        createRecurrenceRequest("MONTHLY", 2, null, List.of(5, 15), LocalDateTime.of(2023, 10, 5, 10, 0), null),
                        "FREQ=MONTHLY;UNTIL=20231005T100000;INTERVAL=2;BYMONTHDAY=5,15"
                ),
                Arguments.of(
                        createRecurrenceRequest("MONTHLY", null, null, List.of(31), null, 10),
                        "FREQ=MONTHLY;COUNT=10;BYMONTHDAY=31"
                ),
                // Edge cases
                Arguments.of(
                        createRecurrenceRequest("DAILY", null, null, null, null, null),
                        "FREQ=DAILY"
                ),
                Arguments.of(
                        createRecurrenceRequest("WEEKLY", 2, null, null, LocalDateTime.of(2023, 10, 5, 10, 0), 5),
                        "FREQ=WEEKLY;UNTIL=20231005T100000;INTERVAL=2"
                ),
                Arguments.of(
                        createRecurrenceRequest("WEEKLY", null, List.of("SUNDAY", "TUESDAY"), null, null, null),
                        "FREQ=WEEKLY;BYDAY=SU,TU"
                )
        );
    }

    private static Stream<Arguments> provideInvalidFrequencyCases() {
        return Stream.of(
                Arguments.of(createRecurrenceRequest("INVALID", 1, null, null, null, null)),
                Arguments.of(createRecurrenceRequest("", 1, null, null, null, null))
        );
    }

    private static Stream<Arguments> provideInvalidByDaysCases() {
        return Stream.of(
                Arguments.of(createRecurrenceRequest("WEEKLY", null, List.of("M"), null, null, null)),
                Arguments.of(createRecurrenceRequest("WEEKLY", null, List.of(""), null, null, null)),
                Arguments.of(createRecurrenceRequest("WEEKLY", null, List.of("T"), null, null, null))
        );
    }

    private static Stream<Arguments> provideIntervalEdgeCases() {
        return Stream.of(
                Arguments.of(
                        createRecurrenceRequest("DAILY", 0, null, null, null, null),
                        "FREQ=DAILY"
                ),
                Arguments.of(
                        createRecurrenceRequest("WEEKLY", -5, List.of("MO"), null, null, null),
                        "FREQ=WEEKLY;BYDAY=MO"
                )
        );
    }

    private static Stream<Arguments> provideRRuleStrings() {
        return Stream.of(
                Arguments.of(
                        "FREQ=WEEKLY;INTERVAL=2;BYDAY=MO,WE,FR;UNTIL=20251231T235959Z",
                        Recurrence.builder()
                                .frequency("WEEKLY")
                                .interval(2)
                                .byDays(List.of("MO", "WE", "FR"))
                                .byMonthDay(null) // Not specified
                                .until(LocalDateTime.of(2025, 12, 31, 23, 59, 59))
                                .count(null)
                                .build()
                ),
                Arguments.of(
                        "FREQ=DAILY;COUNT=10",
                        Recurrence.builder()
                                .frequency("DAILY")
                                .interval(1) // Default interval
                                .byDays(null)
                                .byMonthDay(null)
                                .until(null)
                                .count(10)
                                .build()
                ),
                Arguments.of(
                        "FREQ=MONTHLY;BYMONTHDAY=15,30;UNTIL=20251231T235959Z",
                        Recurrence.builder()
                                .frequency("MONTHLY")
                                .interval(1) // Default interval
                                .byDays(null)
                                .byMonthDay(List.of(15, 30))
                                .until(LocalDateTime.of(2025, 12, 31, 23, 59, 59))
                                .count(null)
                                .build()
                ),
                Arguments.of(
                        "FREQ=WEEKLY;INTERVAL=1;COUNT=5",
                        Recurrence.builder()
                                .frequency("WEEKLY")
                                .interval(1)
                                .byDays(null) // No BYDAY specified
                                .byMonthDay(null)
                                .until(null)
                                .count(5)
                                .build()
                ),
                Arguments.of(
                        "FREQ=MONTHLY;INTERVAL=3;BYDAY=TU,TH;COUNT=12",
                        Recurrence.builder()
                                .frequency("MONTHLY")
                                .interval(3)
                                .byDays(List.of("TU", "TH"))
                                .byMonthDay(null)
                                .until(null)
                                .count(12)
                                .build()
                ),
                Arguments.of(
                        "FREQ=DAILY;INTERVAL=10",
                        Recurrence.builder()
                                .frequency("DAILY")
                                .interval(10)
                                .byDays(null)
                                .byMonthDay(null)
                                .until(null)
                                .count(null)
                                .build()
                ),
                Arguments.of(
                        "FREQ=YEARLY;INTERVAL=1;COUNT=5",
                        Recurrence.builder()
                                .frequency("YEARLY")
                                .interval(1)
                                .byDays(null)
                                .byMonthDay(null)
                                .until(null)
                                .count(5)
                                .build()
                ),
                Arguments.of(
                        "FREQ=WEEKLY;BYDAY=SA,SU;COUNT=20",
                        Recurrence.builder()
                                .frequency("WEEKLY")
                                .interval(1)
                                .byDays(List.of("SA", "SU"))
                                .byMonthDay(null)
                                .until(null)
                                .count(20)
                                .build()
                ),
                Arguments.of(
                        "FREQ=MONTHLY;BYMONTHDAY=1",
                        Recurrence.builder()
                                .frequency("MONTHLY")
                                .interval(1)
                                .byDays(null)
                                .byMonthDay(List.of(1))
                                .until(null)
                                .count(null)
                                .build()
                ),
                Arguments.of(
                        "FREQ=DAILY",
                        Recurrence.builder()
                                .frequency("DAILY")
                                .interval(1)
                                .byDays(null)
                                .byMonthDay(null)
                                .until(null)
                                .count(null)
                                .build()
                )
        );
    }
}