package com.whatstheplan.events.utils;

import com.whatstheplan.events.model.request.EventRequest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class RecurrenceUtilsTest {

    @ParameterizedTest
    @MethodSource("provideValidRecurrenceRequests")
    void generateRRule_ValidCases_ReturnsCorrectRRule(EventRequest.RecurrenceRequest recurrenceRequest, String expectedRrule) {
        String actualRrule = RecurrenceUtils.generateRRule(recurrenceRequest);
        assertEquals(expectedRrule, actualRrule);
    }

    @ParameterizedTest
    @MethodSource("provideInvalidFrequencyCases")
    void generateRRule_InvalidFrequency_ReturnsNull(EventRequest.RecurrenceRequest recurrenceRequest) {
        assertNull(RecurrenceUtils.generateRRule(recurrenceRequest));
    }

    @ParameterizedTest
    @MethodSource("provideInvalidByDaysCases")
    void generateRRule_InvalidByDays_ThrowsException(EventRequest.RecurrenceRequest recurrenceRequest) {
        assertThrows(StringIndexOutOfBoundsException.class, () -> RecurrenceUtils.generateRRule(recurrenceRequest));
    }

    @ParameterizedTest
    @MethodSource("provideIntervalEdgeCases")
    void generateRRule_IntervalEdgeCases_OmitsInterval(EventRequest.RecurrenceRequest recurrenceRequest, String expectedRrule) {
        String actualRrule = RecurrenceUtils.generateRRule(recurrenceRequest);
        assertEquals(expectedRrule, actualRrule);
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
                Arguments.of(createRecurrenceRequest("YEARLY", 1, null, null, null, null)),
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

    private static EventRequest.RecurrenceRequest createRecurrenceRequest(
            String frequency, Integer interval, List<String> byDays, List<Integer> byMonthDay, LocalDateTime until, Integer count) {
        return EventRequest.RecurrenceRequest.builder()
                .frequency(frequency)
                .interval(interval)
                .byDays(byDays)
                .byMonthDay(byMonthDay)
                .until(until)
                .count(count)
                .build();
    }
}