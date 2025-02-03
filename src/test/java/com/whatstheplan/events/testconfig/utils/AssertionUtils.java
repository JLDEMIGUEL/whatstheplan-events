package com.whatstheplan.events.testconfig.utils;

import com.whatstheplan.events.model.entities.Event;
import com.whatstheplan.events.model.entities.EventCategories;
import com.whatstheplan.events.model.request.EventRequest;
import com.whatstheplan.events.model.response.EventResponse;
import lombok.experimental.UtilityClass;

import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.whatstheplan.events.testconfig.BaseIntegrationTest.USER_ID;
import static com.whatstheplan.events.utils.RecurrenceUtils.parseRRule;
import static com.whatstheplan.events.utils.RecurrenceUtilsTest.assertRecurrenceEquals;
import static org.assertj.core.api.Assertions.assertThat;

@UtilityClass
public class AssertionUtils {

    public static void assertEventResponse(
            EventRequest request,
            String imageName,
            EventResponse eventResponse) {
        assertThat(eventResponse.getId()).isNotNull();
        assertThat(eventResponse.getTitle()).isEqualTo(request.getTitle());
        assertThat(eventResponse.getDescription()).isEqualTo(request.getDescription());
        assertThat(eventResponse.getDateTime()).isEqualToIgnoringNanos(request.getDateTime());
        assertThat(eventResponse.getDuration()).isEqualTo(request.getDuration());
        assertThat(eventResponse.getLocation()).isEqualTo(request.getLocation());
        assertThat(eventResponse.getCapacity()).isEqualTo(request.getCapacity());
        assertThat(eventResponse.getImageKey()).contains(imageName);
        assertThat(eventResponse.getOrganizerId()).isEqualTo(USER_ID);
        //assertThat(eventResponse.getOrganizerUsername()).isEqualTo(null); TODO
        //assertThat(eventResponse.getOrganizerEmail()).isEqualTo(null); TODO
        assertThat(eventResponse.getCreatedDate()).isNotNull();
        assertThat(eventResponse.getLastModifiedDate()).isNotNull();

        assertThat(eventResponse.getActivityTypes()).containsAll(request.getActivityTypes());
        assertThat(eventResponse.getActivityTypes().size()).isEqualTo(request.getActivityTypes().size());

        if (request.getRecurrence() != null) {
            assertRecurrenceEquals(request.getRecurrence(), eventResponse.getRecurrence());
        } else {
            assertThat(eventResponse.getRecurrence()).isNull();
        }
    }

    public static void assertEventResponse(Event eventEntity, List<EventCategories> eventCategories,
                                           EventResponse eventResponse) {
        assertThat(eventResponse.getId()).isNotNull();
        assertThat(eventResponse.getTitle()).isEqualTo(eventEntity.getTitle());
        assertThat(eventResponse.getDescription()).isEqualTo(eventEntity.getDescription());
        assertThat(eventResponse.getDateTime()).isEqualToIgnoringNanos(eventEntity.getDateTime());
        assertThat(eventResponse.getDuration()).isEqualTo(eventEntity.getDuration());
        assertThat(eventResponse.getLocation()).isEqualTo(eventEntity.getLocation());
        assertThat(eventResponse.getCapacity()).isEqualTo(eventEntity.getCapacity());
        assertThat(eventResponse.getImageKey()).contains(eventEntity.getImageKey());
        assertThat(eventResponse.getOrganizerId()).isEqualTo(USER_ID);
        //assertThat(eventResponse.getOrganizerUsername()).isEqualTo(null); TODO
        //assertThat(eventResponse.getOrganizerEmail()).isEqualTo(null); TODO
        assertThat(eventResponse.getCreatedDate()).isNotNull();
        assertThat(eventResponse.getLastModifiedDate()).isNotNull();

        assertThat(eventResponse.getActivityTypes())
                .containsAll(eventCategories.stream().map(EventCategories::getActivityType).toList());
        assertThat(eventResponse.getActivityTypes().size()).isEqualTo(eventCategories.size());

        assertRecurrenceEquals(parseRRule(eventEntity.getRecurrence()), eventResponse.getRecurrence());
    }

    public static void assertEventEntity(
            EventRequest request,
            String imageName,
            Event event,
            List<EventCategories> eventCategories) {
        assertThat(event.getId()).isNotNull();
        assertThat(event.getTitle()).isEqualTo(request.getTitle());
        assertThat(event.getDescription()).isEqualTo(request.getDescription());
        assertThat(event.getDateTime()).isEqualToIgnoringNanos(request.getDateTime());
        assertThat(event.getDuration()).isEqualTo(request.getDuration());
        assertThat(event.getLocation()).isEqualTo(request.getLocation());
        assertThat(event.getCapacity()).isEqualTo(request.getCapacity());
        assertThat(event.getImageKey()).contains(imageName);
        assertThat(event.getOrganizerId()).isEqualTo(USER_ID);
        //assertThat(event.getOrganizerUsername()).isEqualTo(null); TODO
        //assertThat(event.getOrganizerEmail()).isEqualTo(null); TODO
        assertThat(event.getCreatedDate()).isNotNull();
        assertThat(event.getLastModifiedDate()).isNotNull();

        if (request.getRecurrence() != null) {
            assertThat(event.getRecurrence()).contains("FREQ=" + request.getRecurrence().getFrequency(),
                    "UNTIL=" + request.getRecurrence().getUntil().format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                    "BYDAY=" + String.join(",", request.getRecurrence().getByDays()));
        } else {
            assertThat(event.getRecurrence()).isNull();
        }

        assertThat(eventCategories.stream().map(EventCategories::getActivityType).toList())
                .containsAll(request.getActivityTypes());
        assertThat(eventCategories.size()).isEqualTo(request.getActivityTypes().size());

        assertThat(eventCategories)
                .allSatisfy(category -> assertThat(category.getEventId()).isEqualTo(event.getId()));
        assertThat(eventCategories)
                .allSatisfy(category -> assertThat(category.getId()).isNotNull());
        assertThat(eventCategories)
                .allSatisfy(category -> assertThat(category.getCreatedDate()).isNotNull());
        assertThat(eventCategories)
                .allSatisfy(category -> assertThat(category.getLastModifiedDate()).isNotNull());
    }

    public static void assertEventEntity(
            Event expectedEvent,
            Event actualEvent,
            List<EventCategories> expectedEventCategories,
            List<EventCategories> actualEventCategories) {
        assertThat(actualEvent.getId()).isNotNull();
        assertThat(actualEvent.getTitle()).isEqualTo(expectedEvent.getTitle());
        assertThat(actualEvent.getDescription()).isEqualTo(expectedEvent.getDescription());
        assertThat(actualEvent.getDateTime()).isEqualToIgnoringNanos(expectedEvent.getDateTime());
        assertThat(actualEvent.getDuration()).isEqualTo(expectedEvent.getDuration());
        assertThat(actualEvent.getLocation()).isEqualTo(expectedEvent.getLocation());
        assertThat(actualEvent.getCapacity()).isEqualTo(expectedEvent.getCapacity());
        assertThat(actualEvent.getImageKey()).contains(expectedEvent.getImageKey());
        assertThat(actualEvent.getRecurrence()).isEqualTo(expectedEvent.getRecurrence());
        assertThat(actualEvent.getOrganizerId()).isEqualTo(USER_ID);
        //assertThat(actualEvent.getOrganizerUsername()).isEqualTo(null); TODO
        //assertThat(actualEvent.getOrganizerEmail()).isEqualTo(null); TODO
        assertThat(actualEvent.getCreatedDate()).isNotNull();
        assertThat(actualEvent.getLastModifiedDate()).isNotNull();

        assertThat(actualEventCategories)
                .allSatisfy(category -> assertThat(category.getEventId()).isEqualTo(actualEvent.getId()));
        assertThat(actualEventCategories)
                .allSatisfy(category -> assertThat(category.getId()).isNotNull());
        assertThat(actualEventCategories)
                .allSatisfy(category -> assertThat(category.getCreatedDate()).isNotNull());
        assertThat(actualEventCategories)
                .allSatisfy(category -> assertThat(category.getLastModifiedDate()).isNotNull());
    }
}
