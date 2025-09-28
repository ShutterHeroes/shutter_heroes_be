package com.example.demo.config.log;

import com.example.demo.config.log.dto.LoggingInfo;
import com.example.demo.exceptions.dto.WarnLogData;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

import static java.util.Objects.isNull;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
@RequiredArgsConstructor
public class LoggingFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws IOException, ServletException {
        final String contentType = request.getHeader(HttpHeaders.CONTENT_TYPE);
        if (!isNull(contentType) && (contentType.startsWith(MediaType.MULTIPART_FORM_DATA_VALUE) || contentType.startsWith(MediaType.APPLICATION_FORM_URLENCODED_VALUE))) {
            filterChain.doFilter(request, response);
            return;
        }

        final CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);
        try {
            String trxId = UUID.randomUUID().toString().substring(0, 10);
            MDC.put("trx_id", trxId);
            final LoggingInfo requestLoggingInfo = new LoggingInfo(request, cachedRequest);
            log.info("REQUEST", StructuredArguments.keyValue("request", requestLoggingInfo));
        } catch (Exception e) {
            log.error("REQUEST_LOGGING_EXCEPTION_CATCH", StructuredArguments.keyValue("exception", new WarnLogData("REQUEST_LOGGING_FAIL", "request logging fail", e)));
        } finally {
            filterChain.doFilter(cachedRequest, response);
            MDC.clear();
        }
    }
}
