package com.whatstheplan.events.testconfig.rabbit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.messaging.Message;

import java.nio.charset.StandardCharsets;

public class RabbitUtils {

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    public static <T> T poll(OutputDestination output, String bindingName, Class<T> clazz) {
        Message<?> message = output.receive(1000L, bindingName);
        try {
            return OBJECT_MAPPER.readValue(new String((byte[]) message.getPayload(), StandardCharsets.UTF_8), clazz);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
