package com.whatstheplan.events.exceptions;

public class UploadImageToS3Exception extends RuntimeException {
    public UploadImageToS3Exception(String message, Throwable cause) {
        super(message, cause);
    }
}
