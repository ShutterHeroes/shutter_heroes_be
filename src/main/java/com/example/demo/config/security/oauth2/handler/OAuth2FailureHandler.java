package com.example.demo.config.security.oauth2.handler;

import com.example.demo.exceptions.dto.WarnLogData;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {
    @Value("${spring.security.oauth2.uri.base}")
    private String REDIRECT_URL;

    @Override
    public void onAuthenticationFailure(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException exception
    ) throws IOException {
        String errorCode = mapExceptionToErrorCode(exception);
        String redirectUrl = UriComponentsBuilder.fromUriString(REDIRECT_URL)
            .queryParam("error", errorCode)
            .build().toUriString();
        log.error("OAUTH2_FAILURE_HANDLE", StructuredArguments.keyValue("exception", new WarnLogData("OAUTH2_FAILURE", "request oauth2 fail", exception)));
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    private String mapExceptionToErrorCode(AuthenticationException e) {
        if (e instanceof OAuth2AuthenticationException) {
            String msg = e.getMessage();
            if (msg.contains("지원하지 않는")) return "PROVIDER_NOT_SUPPORTED";
            if (msg.contains("동의")) return "CONSENT_REQUIRED";
        }
        return "OAUTH2_ERROR";
    }
}
