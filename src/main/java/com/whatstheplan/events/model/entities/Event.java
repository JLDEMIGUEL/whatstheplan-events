package com.whatstheplan.events.model.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table
public class Event extends AbstractAuditingEntity<UUID> {
    @Id
    private UUID id;
    private String title;
    private String description;
    private LocalDateTime dateTime;
    private Duration duration;
    private String location;
    private int capacity;
    private String imageKey;
    private UUID organizerId;
}
