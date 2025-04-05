package com.whatstheplan.events.testconfig.utils;

import com.whatstheplan.events.model.entities.Category;
import com.whatstheplan.events.model.entities.Event;
import com.whatstheplan.events.model.request.EventRequest;
import com.whatstheplan.events.model.response.DetailedEventResponse;
import com.whatstheplan.events.model.response.EventResponse;
import lombok.experimental.UtilityClass;

import java.util.List;

import static com.whatstheplan.events.testconfig.BaseIntegrationTest.USER_ID;
import static java.lang.Boolean.TRUE;
import static org.assertj.core.api.Assertions.assertThat;

@UtilityClass
public class AssertionUtils {

    public static void assertEventResponse(
            EventRequest request,
            String imageName,
            EventResponse eventResponse,
            Integer registrations) {
        assertThat(eventResponse.getId()).isNotNull();
        assertThat(eventResponse.getTitle()).isEqualTo(request.getTitle());
        assertThat(eventResponse.getDescription()).isEqualTo(request.getDescription());
        assertThat(eventResponse.getDateTime()).isEqualToIgnoringNanos(request.getDateTime());
        assertThat(eventResponse.getDuration()).isEqualTo(request.getDuration());
        assertThat(eventResponse.getLocation()).isEqualTo(request.getLocation());
        assertThat(eventResponse.getCapacity()).isEqualTo(request.getCapacity());
        assertThat(eventResponse.getImageKey()).contains(imageName);
        assertThat(eventResponse.getOrganizerId()).isEqualTo(USER_ID);
        assertThat(eventResponse.getCreatedDate()).isNotNull();
        assertThat(eventResponse.getLastModifiedDate()).isNotNull();
        assertThat(eventResponse.getRegistrations()).isEqualTo(registrations);
        assertThat(eventResponse.getIsOwnedByUser()).isEqualTo(TRUE);

        assertThat(eventResponse.getActivityTypes()).containsAll(request.getActivityTypes());
        assertThat(eventResponse.getActivityTypes().size()).isEqualTo(request.getActivityTypes().size());
    }

    public static void assertEventResponse(Event eventEntity, List<Category> categories,
                                           EventResponse eventResponse, Integer registrations) {
        assertThat(eventResponse.getId()).isNotNull();
        assertThat(eventResponse.getTitle()).isEqualTo(eventEntity.getTitle());
        assertThat(eventResponse.getDescription()).isEqualTo(eventEntity.getDescription());
        assertThat(eventResponse.getDateTime()).isEqualToIgnoringNanos(eventEntity.getDateTime());
        assertThat(eventResponse.getDuration()).isEqualTo(eventEntity.getDuration());
        assertThat(eventResponse.getLocation()).isEqualTo(eventEntity.getLocation());
        assertThat(eventResponse.getCapacity()).isEqualTo(eventEntity.getCapacity());
        assertThat(eventResponse.getImageKey()).contains(eventEntity.getImageKey());
        assertThat(eventResponse.getOrganizerId()).isEqualTo(USER_ID);
        assertThat(eventResponse.getCreatedDate()).isNotNull();
        assertThat(eventResponse.getLastModifiedDate()).isNotNull();
        assertThat(eventResponse.getRegistrations()).isEqualTo(registrations);
        assertThat(eventResponse.getIsOwnedByUser()).isEqualTo(TRUE);

        assertThat(eventResponse.getActivityTypes())
                .containsAll(categories.stream().map(Category::getName).toList());
        assertThat(eventResponse.getActivityTypes().size()).isEqualTo(categories.size());
    }

    public static void assertDetailedEventResponse(Event eventEntity, List<Category> categories,
                                                   DetailedEventResponse eventResponse, Integer registrations,
                                                   boolean isRegistered, String organizerUsername) {

        assertEventResponse(eventEntity, categories, eventResponse, registrations);

        assertThat(eventResponse.getIsRegistered()).isEqualTo(isRegistered);
        assertThat(eventResponse.getOrganizerUsername()).isEqualTo(organizerUsername);
    }

    public static void assertEventEntity(
            EventRequest request,
            String imageName,
            Event event,
            List<Category> categories,
            Integer registrations) {
        assertThat(event.getId()).isNotNull();
        assertThat(event.getTitle()).isEqualTo(request.getTitle());
        assertThat(event.getDescription()).isEqualTo(request.getDescription());
        assertThat(event.getDateTime()).isEqualToIgnoringNanos(request.getDateTime());
        assertThat(event.getDuration()).isEqualTo(request.getDuration());
        assertThat(event.getLocation()).isEqualTo(request.getLocation());
        assertThat(event.getCapacity()).isEqualTo(request.getCapacity());
        assertThat(event.getImageKey()).contains(imageName);
        assertThat(event.getOrganizerId()).isEqualTo(USER_ID);
        assertThat(event.getCreatedDate()).isNotNull();
        assertThat(event.getLastModifiedDate()).isNotNull();
        assertThat(event.getRegistrations()).isEqualTo(registrations);

        assertThat(categories.size()).isEqualTo(request.getActivityTypes().size());
        assertThat(categories.stream().map(Category::getName).toList())
                .containsAll(request.getActivityTypes());
        assertThat(categories)
                .allSatisfy(category -> assertThat(category.getId()).isNotNull());
    }

    public static void assertEventEntity(
            Event expectedEvent,
            Event actualEvent,
            List<Category> expectedCategories,
            List<Category> actualCategories) {
        assertThat(actualEvent.getId()).isNotNull();
        assertThat(actualEvent.getTitle()).isEqualTo(expectedEvent.getTitle());
        assertThat(actualEvent.getDescription()).isEqualTo(expectedEvent.getDescription());
        assertThat(actualEvent.getDateTime()).isEqualToIgnoringNanos(expectedEvent.getDateTime());
        assertThat(actualEvent.getDuration()).isEqualTo(expectedEvent.getDuration());
        assertThat(actualEvent.getLocation()).isEqualTo(expectedEvent.getLocation());
        assertThat(actualEvent.getCapacity()).isEqualTo(expectedEvent.getCapacity());
        assertThat(actualEvent.getImageKey()).contains(expectedEvent.getImageKey());
        assertThat(actualEvent.getOrganizerId()).isEqualTo(USER_ID);
        assertThat(actualEvent.getCreatedDate()).isNotNull();
        assertThat(actualEvent.getLastModifiedDate()).isNotNull();
        assertThat(actualEvent.getRegistrations()).isEqualTo(expectedEvent.getRegistrations());

        assertThat(actualCategories.size()).isEqualTo(expectedCategories.size());
        assertThat(actualCategories.stream().map(Category::getName).toList())
                .containsAll(expectedCategories.stream().map(Category::getName).toList());
        assertThat(actualCategories)
                .allSatisfy(category -> assertThat(category.getId()).isNotNull());
    }
}
