package com.whatstheplan.events.client.user;

import com.whatstheplan.events.client.user.response.BasicUserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.UUID;


@Slf4j
@Component
@RequiredArgsConstructor
public class UserClient {

    private final WebClient webClient;

    public Mono<BasicUserResponse> getUserBasicInfo(UUID userId) {
        log.info("Retrieving username for userId: {}", userId);
        return webClient.get()
                .uri("/users-info/{userId}", userId)
                .retrieve()
                .bodyToMono(BasicUserResponse.class)
                .doOnNext(response -> log.info("Successfully retrieved username for user id {}: {}", userId, response.getUsername()));
    }
}
