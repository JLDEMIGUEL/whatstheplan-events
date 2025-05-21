package com.whatstheplan.events.model.email;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuccessfulRegistrationEmail {
    private String username;
    private String email;
    private EventEmailData event;
}
