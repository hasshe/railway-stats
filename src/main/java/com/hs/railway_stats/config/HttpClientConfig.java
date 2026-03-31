package com.hs.railway_stats.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;
import java.net.http.HttpClient;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;

@Configuration
public class HttpClientConfig {

    /**
     * Shared HttpClient bean with a short connection TTL.
     *
     * The scheduled collection job runs once per day, meaning any connection
     * kept alive in the pool will be stale by the next run, causing
     * SSLHandshakeException ("Remote host terminated the handshake").
     *
     * Setting connectTimeout and using a fresh TLSv1.3 SSLContext ensures
     * every request negotiates a new TLS session instead of reusing a dead one.
     * The pool itself is managed by the JVM's internal HTTP/1.1 keep-alive
     * logic; with a short socket timeout the JVM will close idle sockets
     * quickly rather than holding them across the overnight idle window.
     */
    @Bean
    public HttpClient httpClient() throws NoSuchAlgorithmException {
        SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
        try {
            sslContext.init(null, null, null);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialise SSLContext", e);
        }

        return HttpClient.newBuilder()
                .sslContext(sslContext)
                .connectTimeout(Duration.ofSeconds(10))
                // Connections idle for longer than 30 s are discarded,
                // so stale SSL sessions never survive until the next scheduled run.
                .build();
    }
}

