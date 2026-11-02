package com.adaptiveplanner.planner.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class WebSecurityConfiguration {
    @Bean
    CookieCsrfTokenRepository csrfTokenRepository(AuthProperties authProperties) {
        CookieCsrfTokenRepository csrfTokens = new CookieCsrfTokenRepository();
        csrfTokens.setCookieName("ATP-XSRF-TOKEN");
        csrfTokens.setCookieCustomizer(cookie -> cookie.path("/").httpOnly(false).sameSite("Lax")
                .secure(authProperties.isCookieSecure()));
        return csrfTokens;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder decoder, AuthProperties authProperties,
                                            CorsConfigurationSource corsConfigurationSource,
                                            CookieCsrfTokenRepository csrfTokens) throws Exception {
        CsrfTokenRequestAttributeHandler csrfRequestHandler = new CsrfTokenRequestAttributeHandler();

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.csrfTokenRepository(csrfTokens)
                        .csrfTokenRequestHandler(csrfRequestHandler)
                        .withObjectPostProcessor(new ObjectPostProcessor<CsrfFilter>() {
                            @Override
                            public <O extends CsrfFilter> O postProcess(O filter) {
                                filter.setAccessDeniedHandler((request, response, exception) -> {
                                    csrfTokens.saveToken(null, request, response);
                                    if (request.getRequestURI().startsWith("/api/")) {
                                        response.setStatus(HttpStatus.FORBIDDEN.value());
                                        response.setContentType("application/json");
                                        response.getWriter().write("{\"error\":\"CSRF token missing or invalid. Refresh the security token and retry.\"}");
                                        return;
                                    }
                                    if ("POST".equalsIgnoreCase(request.getMethod())
                                            && "/subjects".equals(request.getRequestURI())) {
                                        response.sendRedirect("/subjects/new?csrfExpired");
                                        return;
                                    }
                                    response.sendRedirect("/forbidden?csrfExpired");
                                });
                                return filter;
                            }
                        }))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/login", "/login/**", "/styles.css", "/error", "/forbidden").permitAll()
                        .requestMatchers("/api/auth/csrf", "/api/auth/send-otp", "/api/auth/verify-otp",
                                "/api/auth/logout").permitAll()
                        .anyRequest().authenticated())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .securityContext(context -> context.securityContextRepository(new NullSecurityContextRepository()))
                .requestCache(requestCache -> requestCache.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .exceptionHandling(exceptions -> exceptions
                        .accessDeniedPage("/forbidden")
                        .authenticationEntryPoint((request, response, exception) -> {
                            if (request.getRequestURI().startsWith("/api/")) {
                                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                                response.setContentType("application/json");
                                response.getWriter().write("{\"error\":\"Authentication required.\"}");
                            } else {
                                new LoginUrlAuthenticationEntryPoint("/login").commence(request, response, exception);
                            }
                        }))
                .addFilterBefore(new JwtCookieAuthenticationFilter(decoder), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173,http://localhost:5175,http://127.0.0.1:5175}") String allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim).filter(origin -> !origin.isEmpty()).toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
