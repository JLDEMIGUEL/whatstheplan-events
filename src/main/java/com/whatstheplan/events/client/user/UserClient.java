package com.whatstheplan.events.client.user;

import com.whatstheplan.events.client.user.response.BasicUserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static com.whatstheplan.events.config.RedisConfig.CACHE_TTL;


@Slf4j
@Component
@RequiredArgsConstructor
public class UserClient {

    public static final String USER_REDIS_KEY = "user:basic:";
    private final WebClient webClient;
    private final ReactiveRedisTemplate<String, BasicUserResponse> userReactiveRedisTemplate;

    public Mono<BasicUserResponse> getUserBasicInfo(UUID userId) {
        log.info("Retrieving username for userId: {}", userId);
        String cacheKey = USER_REDIS_KEY + userId;
        return userReactiveRedisTemplate.opsForValue().get(cacheKey)
                .doOnNext(response -> log.info("Retrieved from cache for userId: {}", userId))
                .switchIfEmpty(
                        webClient.get()
                                .uri("/users-info/{userId}", userId)
                                .retrieve()
                                .bodyToMono(BasicUserResponse.class)
                                .doOnNext(response -> log.info("Fetched from user service for userId: {}", userId))
                                .flatMap(response ->
                                        userReactiveRedisTemplate.opsForValue()
                                                .set(cacheKey, response, CACHE_TTL)
                                                .thenReturn(response)
                                )
                );
    }
}
