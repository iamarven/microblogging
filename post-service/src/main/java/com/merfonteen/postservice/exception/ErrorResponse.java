package com.merfonteen.postservice.exception;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ErrorResponse {
    private final int status;
    private String message;
    private final String exceptionMessage;
    private final Instant timestamp;

    public ErrorResponse(int status, String message, String exceptionMessage, Instant timestamp) {
        this.status = status;
        this.message = message;
        this.exceptionMessage = exceptionMessage;
        this.timestamp = timestamp;
    }

    public ErrorResponse(int status, String exceptionMessage, Instant timestamp) {
        this.status = status;
        this.exceptionMessage = exceptionMessage;
        this.timestamp = timestamp;
    }
}
