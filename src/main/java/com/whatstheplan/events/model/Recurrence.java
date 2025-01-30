package com.whatstheplan.events.model;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Recurrence {
    @Pattern(regexp = "DAILY|WEEKLY|MONTHLY|YEARLY", message = "Invalid recurrence frequency.")
    private String frequency;

    @Positive(message = "Interval must be a positive number.")
    private Integer interval;

    @Size(min = 1, message = "At least one day must be specified.")
    private List<@Pattern(regexp = "MO|TU|WE|TH|FR|SA|SU", message = "Invalid day abbreviation.") String> byDays;

    private List<@Min(1) @Max(31) Integer> byMonthDay;

    @Future(message = "Recurrence end date must be in the future.")
    private LocalDateTime until;

    @Positive(message = "Count must be a positive number.")
    private Integer count;

    @AssertTrue(message = "Only one of 'until' or 'count' can be specified.")
    public boolean isUntilOrCountExclusive() {
        return (until == null) != (count == null);
    }
}
