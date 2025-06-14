package com.whatstheplan.events.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Filters used for searching events")
public class EventFilterRequest {

    @Schema(description = "Filter by location or city", example = "Madrid")
    private String location;

    @Builder.Default
    @Schema(description = "Minimum duration of the event", example = "PT0H")
    private Duration durationFrom = Duration.ZERO;

    @Builder.Default
    @Schema(description = "Maximum duration of the event", example = "PT24H")
    private Duration durationTo = Duration.ofHours(24);

    @Builder.Default
    @Schema(description = "Start of the date-time range for the event", example = "2025-06-01T00:00:00")
    private LocalDateTime dateTimeFrom = LocalDateTime.now();

    @Builder.Default
    @Schema(description = "End of the date-time range for the event", example = "2025-08-01T00:00:00")
    private LocalDateTime dateTimeTo = LocalDateTime.now().plusMonths(12);

    @Builder.Default
    @Schema(description = "List of activity types to filter by", example = "[\"sports\", \"music\"]")
    private List<String> activityTypes = Collections.emptyList();
}
