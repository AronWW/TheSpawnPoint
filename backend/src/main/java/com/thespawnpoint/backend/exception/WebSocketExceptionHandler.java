package com.thespawnpoint.backend.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.annotation.SendToUser;

import java.util.Map;
import java.util.LinkedHashMap;

@Slf4j
public abstract class WebSocketExceptionHandler {

    @MessageExceptionHandler(ApiException.class)
    @SendToUser("/queue/errors")
    public Map<String, Object> handleApiException(ApiException ex) {
        log.warn("WS ApiException: {}", ex.getMessage());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", ex.getStatus().value());
        body.put("message", ex.getMessage());
        if (ex.getErrorCode() != null && !ex.getErrorCode().isBlank()) {
            body.put("errorCode", ex.getErrorCode());
        }
        return body;
    }

    @MessageExceptionHandler(Exception.class)
    @SendToUser("/queue/errors")
    public Map<String, Object> handleException(Exception ex) {
        log.error("WS unexpected error", ex);
        return Map.of("status", 500, "message", "Internal server error");
    }
}
