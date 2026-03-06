package com.hs.railway_stats.config;

import com.hs.railway_stats.exception.ClaimSubmissionException;
import com.hs.railway_stats.exception.ExternalApiException;
import com.hs.railway_stats.exception.StationNotFoundException;
import com.hs.railway_stats.exception.TripCollectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(StationNotFoundException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponse> handleStationNotFound(StationNotFoundException ex) {
        logger.warn("Station not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("Station not found. Please check the selected route and try again.", ex.getMessage()));
    }

    @ExceptionHandler(TripCollectionException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponse> handleTripCollection(TripCollectionException ex) {
        logger.error("Trip collection failed: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse("Could not collect trip data. Please try again later.", ex.getMessage()));
    }

    @ExceptionHandler(ClaimSubmissionException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponse> handleClaimSubmission(ClaimSubmissionException ex) {
        if (ex.isRateLimited()) {
            logger.warn("Claim rate-limited: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new ErrorResponse("You have too many pending claims. Please wait a moment before trying again.", ex.getMessage()));
        }
        logger.error("Claim submission failed: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse("Claim submission failed. Please try again later.", ex.getMessage()));
    }

    @ExceptionHandler(ExternalApiException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponse> handleExternalApi(ExternalApiException ex) {
        logger.error("External API error (status {}): {}", ex.getStatusCode(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse("An external service is currently unavailable. Please try again later.", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResponseEntity<ErrorResponse> handleAll(Exception ex) {
        logger.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("An unexpected error occurred. Please try again.", null));
    }

    public record ErrorResponse(String userMessage, String details) {
    }
}
