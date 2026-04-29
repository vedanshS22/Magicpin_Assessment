package com.magicpin.mde.util;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
public class RequestIdFilter extends OncePerRequestFilter {
  public static final String HEADER_REQUEST_ID = "X-Request-Id";
  public static final String MDC_REQUEST_ID = "requestId";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    long startNs = System.nanoTime();
    String requestId =
        Optional.ofNullable(request.getHeader(HEADER_REQUEST_ID)).orElse(UUID.randomUUID().toString());
    MDC.put(MDC_REQUEST_ID, requestId);
    response.setHeader(HEADER_REQUEST_ID, requestId);
    try {
      filterChain.doFilter(request, response);
    } finally {
      long ms = (System.nanoTime() - startNs) / 1_000_000;
      log.info(
          "request completed path={} method={} status={} latency_ms={} ts={}",
          request.getRequestURI(),
          request.getMethod(),
          response.getStatus(),
          ms,
          Instant.now().toString());
      MDC.remove(MDC_REQUEST_ID);
    }
  }
}

