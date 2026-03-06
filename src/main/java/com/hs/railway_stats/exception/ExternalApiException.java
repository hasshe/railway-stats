package com.hs.railway_stats.exception;

import lombok.Getter;

@Getter
public class ExternalApiException extends RuntimeException {
    private final int statusCode;

    public ExternalApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public ExternalApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
    }

}

