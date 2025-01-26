package com.whatstheplan.events.model.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(value = {"createdDate", "lastModifiedDate"}, allowGetters = true)
public abstract class AbstractAuditingEntity<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public abstract T getId();

    @CreatedDate
    @Builder.Default
    @Column(value = "created_date")
    private Instant createdDate = Instant.now();

    @LastModifiedDate
    @Builder.Default
    @Column(value = "last_modified_date")
    private Instant lastModifiedDate = Instant.now();
}