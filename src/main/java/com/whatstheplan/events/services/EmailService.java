package com.whatstheplan.events.services;

import com.whatstheplan.events.client.user.UserClient;
import com.whatstheplan.events.client.user.response.BasicUserResponse;
import com.whatstheplan.events.model.email.EventEmailData;
import com.whatstheplan.events.model.email.SuccessfulRegistrationEmail;
import com.whatstheplan.events.model.entities.Event;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    public static final String SUCCESSFUL_REGISTRATION_EMAIL_BINDING = "successfulRegistrationEmail-out-0";
    public static final String EVENT_CANCELLATION_EMAIL_BINDING = "cancelledEvent-out-0";

    private final UserClient userClient;
    private final StreamBridge streamBridge;

    public Mono<Void> sendSuccessfulRegistrationEmail(UUID userId, Event event) {
        return Mono.zip(userClient.getUserBasicInfo(userId), userClient.getUserBasicInfo(event.getOrganizerId()))
                .doOnSuccess(t -> {
                    BasicUserResponse user = t.getT1();
                    BasicUserResponse organizer = t.getT2();
                    log.info("Sending successful registration email request to RabbitMQ with " +
                                    "for user {}, email {}, and event id {}",
                            user.getUsername(), user.getEmail(), event.getId());
                    streamBridge.send(SUCCESSFUL_REGISTRATION_EMAIL_BINDING,
                            MessageBuilder.withPayload(SuccessfulRegistrationEmail.builder()
                                    .email(user.getEmail())
                                    .username(user.getUsername())
                                    .event(EventEmailData.from(event, organizer.getUsername()))
                                    .build()
                            ).build());
                }).then();
    }

    public Mono<Void> sendEventCancellationEmail(UUID userId, Event event, String organizer) {
        return userClient.getUserBasicInfo(userId).doOnSuccess(user -> {
            log.info("Sending event cancellation email request to RabbitMQ with " +
                            "for user {}, email {}, and event id {}",
                    user.getUsername(), user.getEmail(), event.getId());
            streamBridge.send(EVENT_CANCELLATION_EMAIL_BINDING,
                    MessageBuilder.withPayload(SuccessfulRegistrationEmail.builder()
                            .email(user.getEmail())
                            .username(user.getUsername())
                            .event(EventEmailData.from(event, organizer))
                            .build()
                    ).build());
        }).then();
    }
}
