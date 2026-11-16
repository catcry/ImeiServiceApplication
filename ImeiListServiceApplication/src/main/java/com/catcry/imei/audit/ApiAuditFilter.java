package com.catcry.imei.audit;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)   // runs first, so it also logs 429 and 401 responses
public class ApiAuditFilter extends OncePerRequestFilter {

    private static final Logger audit = LoggerFactory.getLogger("API_AUDIT");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/v1/imei/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        ContentCachingResponseWrapper wrapped = new ContentCachingResponseWrapper(res);
        long start = System.currentTimeMillis();
        try {
            chain.doFilter(req, wrapped);
        } finally {
            long ms = System.currentTimeMillis() - start;
            String user = String.valueOf(req.getAttribute("auditUser"));       // set by the receiver after auth
            String batch = String.valueOf(req.getAttribute("auditBatchId"));
            String body = new String(wrapped.getContentAsByteArray(), StandardCharsets.UTF_8);
            if (body.length() > 500) body = body.substring(0, 500) + "...";

            audit.info("ip={} method={} uri={} user={} batch={} status={} ms={} response={}",
                    req.getRemoteAddr(), req.getMethod(), req.getRequestURI(),
                    user, batch, wrapped.getStatus(), ms, body);

            wrapped.copyBodyToResponse();   // required, or the client gets an empty body
        }
    }
}
