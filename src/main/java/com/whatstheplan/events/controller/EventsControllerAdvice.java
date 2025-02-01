package com.whatstheplan.events.controller;

import com.whatstheplan.events.exceptions.EventNotFoundException;
import com.whatstheplan.events.exceptions.FileValidationException;
import com.whatstheplan.events.exceptions.UploadImageToS3Exception;
import com.whatstheplan.events.exceptions.ValidationException;
import com.whatstheplan.events.model.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
public class EventsControllerAdvice {
    @ExceptionHandler(UploadImageToS3Exception.class)
    public ResponseEntity<ErrorResponse> handleValidationException(UploadImageToS3Exception ex) {
        log.warn("{}", ex.getMessage(), ex);
        return ResponseEntity.internalServerError().body(new ErrorResponse("Error while processing image"));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(ValidationException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(FileValidationException.class)
    public ResponseEntity<ErrorResponse> handleFileValidationException(FileValidationException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(EventNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEventNotFoundException(EventNotFoundException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleOtherExceptions(Exception ex) {
        log.error("Error: {}", ex.getMessage(), ex);
        return ResponseEntity.internalServerError().body(new ErrorResponse("Internal Server Error"));
    }
}
