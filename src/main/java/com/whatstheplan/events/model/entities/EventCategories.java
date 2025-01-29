package com.whatstheplan.events.model.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.List;
import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table
public class EventCategories extends AbstractAuditingEntity<UUID> {
    @Id
    private UUID id;
    private String activityType;
    private UUID eventId;

    @SuppressWarnings("unchecked")
    public static List<EventCategories> from(List<String> activitiesTypes, UUID eventId) {
        return (List<EventCategories>) activitiesTypes.stream()
                .map(activityType -> EventCategories.builder()
                        .id(UUID.randomUUID())
                        .activityType(activityType)
                        .eventId(eventId)
                        .isNew(true)
                        .build())
                .toList();
    }
}
