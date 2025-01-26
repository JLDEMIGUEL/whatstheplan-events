package com.whatstheplan.events.utils;

import com.whatstheplan.events.exceptions.MissingEmailInTokenException;
import lombok.experimental.UtilityClass;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Mono;

import java.util.UUID;

@UtilityClass
public class Utils {

    public static Mono<UUID> getUserId() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getName)
                .map(UUID::fromString)
                .switchIfEmpty(Mono.error(new MissingEmailInTokenException("Invalid token, user id not found.", null)));
    }

    public static Mono<String> getUserEmail() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(auth -> ((Jwt) auth.getPrincipal()).getClaimAsString("email"))
                .switchIfEmpty(Mono.error(new MissingEmailInTokenException("Invalid token, email not found.", null)));
    }

}
