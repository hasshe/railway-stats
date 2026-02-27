package com.hs.railway_stats.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record ClaimRequest(
        String id,
        Boolean confirmDuplicate,
        String payoutOption,
        Customer customer,
        String ticketNumber,
        int ticketType,
        String departureStationId,
        String arrivalStationId,
        OffsetDateTime departureDate,
        String comment,
        int status,
        int trainNumber,
        RefundType refundType,
        List<Object> claimReceipts
) {
    public record Customer(
            String id,
            String firstName,
            String surName,
            String city,
            String postalCode,
            String streetNameAndNumber,
            String identityNumber,
            String mobileNumber,
            String email,
            Boolean hasIdentityNumber
    ) {
    }

    public record RefundType(
            String id,
            String name
    ) {
    }
}

