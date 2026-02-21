package com.hs.railway_stats.external;

import java.io.IOException;

import com.hs.railway_stats.external.dto.TripResponse;

public interface RestClient {

    TripResponse callSearch(long originId, long destinationId, String nextToken) throws IOException, InterruptedException;
    
}
