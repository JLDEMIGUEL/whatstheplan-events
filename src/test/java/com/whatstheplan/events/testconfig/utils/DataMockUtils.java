package com.whatstheplan.events.testconfig.utils;

import com.whatstheplan.events.model.Recurrence;
import com.whatstheplan.events.model.entities.Category;
import com.whatstheplan.events.model.entities.Event;
import com.whatstheplan.events.model.request.EventRequest;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.ByteArrayResource;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.whatstheplan.events.testconfig.BaseIntegrationTest.USER_ID;
import static net.fortuna.ical4j.model.WeekDay.Day.MO;
import static net.fortuna.ical4j.model.WeekDay.Day.TH;
import static net.fortuna.ical4j.model.WeekDay.Day.TU;
import static net.fortuna.ical4j.transform.recurrence.Frequency.DAILY;
import static net.fortuna.ical4j.transform.recurrence.Frequency.WEEKLY;

@UtilityClass
public class DataMockUtils {

    public static EventRequest generateEventCreationRequestRecurrent() {
        return generateEventCreationRequest(
                Recurrence.builder()
                        .frequency(DAILY.name())
                        .interval(1)
                        .byDays(List.of(MO.name(), TH.name()))
                        .until(LocalDateTime.now().plusYears(1).withNano(0))
                        .build()
        );
    }

    public static EventRequest generateEventCreationRequestNotRecurrent() {
        return generateEventCreationRequest(null);
    }

    public static EventRequest generateEventCreationRequest(Recurrence recurrence) {
        return EventRequest.builder()
                .title("Test title")
                .description("Test description")
                .dateTime(LocalDateTime.now().plusDays(3).withNano(0))
                .duration(Duration.ofHours(1))
                .location("Test location")
                .capacity(10)
                .recurrence(recurrence)
                .activityTypes(List.of("Soccer", "Baseball", "Hiking"))
                .build();
    }

    public static EventRequest generateEventUpdatedRequest() {
        return EventRequest.builder()
                .title("Updated test title")
                .description("Updated test description")
                .dateTime(LocalDateTime.now().plusDays(5).withNano(0))
                .duration(Duration.ofHours(2))
                .location("Updated test location")
                .capacity(10)
                .recurrence(Recurrence.builder()
                        .frequency(WEEKLY.name())
                        .interval(2)
                        .byDays(List.of(TU.name()))
                        .until(LocalDateTime.now().plusYears(3).withNano(0))
                        .build())
                .activityTypes(List.of("Climbing", "Outdoors", "Food & Dining"))
                .build();
    }

    public static Event generateEventEntity() {
        return Event.builder()
                .id(UUID.randomUUID())
                .title("Test title")
                .description("Test description")
                .dateTime(LocalDateTime.now().plusDays(3).withNano(0))
                .duration(Duration.ofHours(1))
                .location("Test location")
                .capacity(10)
                .imageKey("image.webp")
                .organizerId(USER_ID)
                .recurrence("FREQ=MONTHLY;BYMONTHDAY=15,30;UNTIL=20251231T235959Z")
                .isNew(true)
                .build();
    }

    public static List<Category> generateEventCategories(UUID eventId) {
        return List.of(
                generateEventCategory("Soccer", eventId),
                generateEventCategory("Baseball", eventId),
                generateEventCategory("Hiking", eventId));
    }

    public static Category generateEventCategory(String Soccer, UUID eventId) {
        return Category.builder()
                .id(UUID.randomUUID())
                .name(Soccer)
                .isNew(true)
                .build();
    }

    @NotNull
    public static ByteArrayResource generateImage(byte[] content, String filename) {
        return new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
    }
}
