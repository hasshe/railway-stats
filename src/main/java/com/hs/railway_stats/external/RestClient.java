package com.hs.railway_stats.external;

import java.io.IOException;

public interface RestClient {

    TripResponse callSearch(long originId, long destinationId, String nextToken) throws IOException, InterruptedException;
    
}
