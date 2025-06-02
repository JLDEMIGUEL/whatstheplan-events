package com.whatstheplan.events.model.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Standard error response with failure reason")
public class ErrorResponse {

    @Schema(description = "Explanation of the error", example = "User not found")
    private String reason;
}
