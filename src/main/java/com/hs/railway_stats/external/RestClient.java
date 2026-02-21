package com.hs.railway_stats.external;

import java.io.IOException;
import java.time.OffsetDateTime;

public interface RestClient {

    TripResponse callSearch(OffsetDateTime startOfDay, long originId, long destinationId, String nextToken) throws IOException, InterruptedException;
    
}
