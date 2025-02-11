package com.whatstheplan.events.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventFilterRequest {

    private String location;

    private Duration durationFrom;
    private Duration durationTo;

    @Builder.Default
    private LocalDateTime dateTimeFrom = LocalDateTime.now();
    @Builder.Default
    private LocalDateTime dateTimeTo = LocalDateTime.now().plusMonths(2);

    private Integer capacityMin;
    private Integer capacityMax;

    @Builder.Default
    private List<String> activityTypes = Collections.emptyList();
}
