package com.hs.railway_stats.exception;

import lombok.Getter;

@Getter
public class ClaimSubmissionException extends RuntimeException {
    private final boolean rateLimited;

    public ClaimSubmissionException(String message, Throwable cause, boolean rateLimited) {
        super(message, cause);
        this.rateLimited = rateLimited;
    }

}

