package com.hs.railway_stats.view.util;

import com.vaadin.flow.server.VaadinRequest;

public final class VaadinRequestUtils {

    private VaadinRequestUtils() {}

    public static String getClientIp() {
        VaadinRequest request = VaadinRequest.getCurrent();
        if (request == null) return "unknown";
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

