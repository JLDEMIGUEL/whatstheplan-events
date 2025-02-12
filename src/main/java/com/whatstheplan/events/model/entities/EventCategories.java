package com.whatstheplan.events.model.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table
public class EventCategories implements Persistable<UUID> {

    @Id
    private UUID id;
    private UUID eventId;
    private UUID categoryId;

    @Transient
    private boolean isNew;

    public static EventCategories from(UUID eventId, UUID categoryId) {
        return EventCategories.builder()
                .id(UUID.randomUUID())
                .eventId(eventId)
                .categoryId(categoryId)
                .isNew(true)
                .build();
    }

    @Transient
    public boolean isNew() {
        return this.isNew || getId() == null;
    }

}
