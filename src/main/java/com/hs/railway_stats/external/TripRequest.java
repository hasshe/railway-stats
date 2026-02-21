package com.hs.railway_stats.external;

public record TripRequest(
        long originId,
        long destinationId,
        String nextToken,
        String dateTime,
        boolean searchForArrival,
        boolean includeAllMovingoOperators
) {}