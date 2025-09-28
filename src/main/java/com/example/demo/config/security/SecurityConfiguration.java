package com.example.demo.config.security;

import com.example.demo.config.security.jwt.JwtAuthenticationFilter;
import com.example.demo.config.security.oauth2.CustomOAuth2UserService;
import com.example.demo.config.security.oauth2.handler.OAuth2FailureHandler;
import com.example.demo.config.security.oauth2.handler.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.filter.CorsFilter;

@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailureHandler oAuth2FailureHandler;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ExceptionHandlerFilter exceptionHandlerFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Bean
    @Order(1)
    public SecurityFilterChain healthCheckApiFilterChain(HttpSecurity http) throws Exception {
        return http
            .securityMatcher("/health-check")
            .authorizeHttpRequests(auth -> {
                auth.requestMatchers("/health-check").permitAll();
            })
            .build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/**")
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)) // H2 콘솔을 위한 iframe 허용
            .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(HttpMethod.GET, "/").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/login").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/oauth2/authorization/kakao").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/login/oauth2/code/kakao").permitAll();

                    // 유저 관련 API
                    auth.requestMatchers(HttpMethod.POST, "/api/v1/users/register").permitAll();
                    auth.requestMatchers(HttpMethod.POST, "/api/v1/users/login").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/api/v1/users/exists").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/api/v1/users").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/api/v1/users/me").authenticated();

                // Swagger UI 경로 허용 (기본 생성 문서)
                    auth.requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/swagger-resources/**", "/webjars/**", "/favicon.ico").permitAll();

                    auth.anyRequest().authenticated();
                }
            )
            .sessionManagement((sessionManagement) ->
                sessionManagement.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            )
            .oauth2Login(customConfig -> customConfig
                .successHandler(oAuth2SuccessHandler)
                .failureHandler(oAuth2FailureHandler)
                .userInfoEndpoint(endPointConfig -> endPointConfig.userService(customOAuth2UserService))
            )
            .exceptionHandling(exceptionHandling ->
                exceptionHandling.authenticationEntryPoint(jwtAuthenticationEntryPoint)
            )
            .addFilterBefore(corsFilter(), SecurityContextHolderFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(exceptionHandlerFilter, CorsFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOriginPattern("*"); // 허용할 도메인 설정
        configuration.addAllowedHeader("*"); // 모든 헤더 허용
        configuration.addAllowedMethod("*"); // 모든 HTTP 메소드 허용
        configuration.setAllowCredentials(true); // 자격증명 허용

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    @Bean
    public CorsFilter corsFilter() {
        return new CorsFilter(corsConfigurationSource());
    }

    /**
     * Security filter chain에서는 한글 인코딩이 적용안되어서, 필터를 통해 적용되도록 함
     *
     * @return CharacterEncodingFilter
     */
    @Bean
    public CharacterEncodingFilter characterEncodingFilter() {
        CharacterEncodingFilter filter = new CharacterEncodingFilter();
        filter.setEncoding("UTF-8");
        filter.setForceEncoding(true);

        return filter;
    }
}
