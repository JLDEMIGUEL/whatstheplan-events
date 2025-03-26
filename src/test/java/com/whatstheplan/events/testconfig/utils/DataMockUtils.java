package com.whatstheplan.events.testconfig.utils;

import com.whatstheplan.events.model.entities.Category;
import com.whatstheplan.events.model.entities.Event;
import com.whatstheplan.events.model.entities.Registration;
import com.whatstheplan.events.model.request.EventRequest;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.ByteArrayResource;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.whatstheplan.events.testconfig.BaseIntegrationTest.USER_ID;

@UtilityClass
public class DataMockUtils {

    public static LocalDateTime TODAY = LocalDateTime.now().withNano(0).withSecond(0).withMinute(0);

    public static EventRequest generateEventCreationRequest() {
        return EventRequest.builder()
                .title("Test title")
                .description("Test description")
                .dateTime(TODAY.plusDays(3).withNano(0))
                .duration(Duration.ofHours(1))
                .location("Test location")
                .capacity(10)
                .activityTypes(List.of("Soccer", "Baseball", "Hiking"))
                .build();
    }

    public static EventRequest generateEventUpdatedRequest() {
        return EventRequest.builder()
                .title("Updated test title")
                .description("Updated test description")
                .dateTime(TODAY.plusDays(5).withNano(0))
                .duration(Duration.ofHours(2))
                .location("Updated test location")
                .capacity(10)
                .activityTypes(List.of("Climbing", "Outdoors", "Food & Dining"))
                .build();
    }

    public static Event generateEventEntity() {
        return Event.builder()
                .id(UUID.randomUUID())
                .title("Test title")
                .description("Test description")
                .dateTime(TODAY.plusDays(3).withNano(0))
                .duration(Duration.ofHours(1))
                .location("Test location")
                .capacity(10)
                .imageKey("image.webp")
                .organizerId(USER_ID)
                .registrations(5)
                .isNew(true)
                .build();
    }

    public static List<Category> generateEventCategories() {
        return List.of(
                generateEventCategory("Soccer"),
                generateEventCategory("Baseball"),
                generateEventCategory("Hiking"));
    }

    public static Category generateEventCategory(String name) {
        return Category.builder()
                .id(UUID.randomUUID())
                .name(name)
                .isNew(true)
                .build();
    }

    public static Registration generateRegistration(UUID eventId) {
        return Registration.builder()
                .id(UUID.randomUUID())
                .userId(USER_ID)
                .eventId(eventId)
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
