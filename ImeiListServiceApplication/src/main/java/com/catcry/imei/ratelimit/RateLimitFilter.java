package com.catcry.imei.ratelimit;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    //private static final long MIN_INTERVAL_MS = 1000L;
    private final long minIntervalMs;
    private final ConcurrentHashMap<String, Long> lastSeen = new ConcurrentHashMap<>();


    public RateLimitFilter(@Value("${imei.ratelimit.min-interval-ms}") long minIntervalMs) {
        this.minIntervalMs = minIntervalMs;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/v1/imei/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String key = req.getRemoteAddr();
        long now = System.currentTimeMillis();

        boolean allowed = tryAcquire(key, now);
        if (!allowed) {
            res.setStatus(429);
            res.setHeader("Retry-After", "1");
            res.setContentType("application/json");
            res.getWriter().write("{\"status\":\"RATE_LIMITED\",\"message\":\"Too many requests\"}");
            return;
        }
        chain.doFilter(req, res);
    }

    private boolean tryAcquire(String key, long now) {
        final boolean[] ok = {false};
        lastSeen.compute(key, (k, last) -> {
            if (last == null || now - last >= minIntervalMs) {
                ok[0] = true;
                return now;
            }
            return last;
        });
        return ok[0];
    }
}
