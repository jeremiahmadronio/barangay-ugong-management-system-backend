package com.barangay.zshared.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import static com.barangay.zshared.security.SecurityConstant.*;

/**
 * The main security rulebook of the application.
 * Defines which endpoints are public, which require a specific role,
 * and which require a specific department assignment.
 * Also sets up CORS and returns clean JSON error responses
 * for unauthorized and forbidden requests.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;


    /**
     * Creates an access rule for department-specific endpoints.
     * ROOT_ADMIN and SYSTEM_ADMIN can always access. ADMIN, STAFF, and OFFICER need the matching department authority.
     */
    private static AuthorizationManager<RequestAuthorizationContext> deptAccess(String dept) {
        return new WebExpressionAuthorizationManager(
                "hasAnyRole('ROOT_ADMIN', 'SYSTEM_ADMIN') or " +
                        "(hasAnyRole('ADMIN', 'STAFF', 'OFFICER') and (hasAuthority('DEPT_" + dept + "') or hasAuthority('DEPT_" + dept + "_TAGAPAMAYAPA')))"
        );
    }

    /**
     * Configures all HTTP security rules.
     * Disables CSRF, sets up CORS, defines endpoint permissions,
     * enforces stateless sessions, and registers the JWT filter.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers("/api/v1/users/me", "/api/v1/users/profile", "/api/v1/users/profile/**", "/api/v1/users/change-password", "/api/v1/users/global-mfa", "/api/v1/users/totp/**").authenticated()
                        .requestMatchers(ROOT_ADMIN_ENDPOINTS).hasAnyRole(ROLE_ROOT_ADMIN, ROLE_SYSTEM_ADMIN)
                        .requestMatchers(ADMIN_ENDPOINTS).hasAnyRole(ROLE_ADMIN, ROLE_ROOT_ADMIN, ROLE_SYSTEM_ADMIN)
                        .requestMatchers(VAWC_ENDPOINTS).access(deptAccess("VAWC"))
                        .requestMatchers(LUPON_ENDPOINTS).access(deptAccess("LUPON"))
                        .requestMatchers(BLOTTER_ENDPOINTS).access(deptAccess("BLOTTER"))
                        .requestMatchers(BCPC_ENDPOINTS).access(deptAccess("BCPC"))
                        .requestMatchers(CLEARANCE_ENDPOINTS).access(deptAccess("CLEARANCE"))
                        .requestMatchers(SHARED_CASE_ENDPOINTS).hasAnyRole(ROLE_ROOT_ADMIN, ROLE_SYSTEM_ADMIN, ROLE_ADMIN, ROLE_STAFF, ROLE_OFFICER)
                        .anyRequest().authenticated()
                )
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) ->
                                writeErrorResponse(res, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication is required"))
                        .accessDeniedHandler((req, res, e) ->
                                writeErrorResponse(res, HttpStatus.FORBIDDEN, "ACCESS_DENIED", "You don't have permission to access this resource"))
                )
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }


    /** Sets up which origins, methods, and headers are allowed for cross-origin requests. */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(ALLOWED_ORIGINS));
        config.setAllowedMethods(Arrays.asList(ALLOWED_METHODS));
        config.setAllowedHeaders(Arrays.asList(ALLOWED_HEADERS));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }


    /** Builds and sends a JSON error response when a request is blocked by security. */
    private void writeErrorResponse(HttpServletResponse response, HttpStatus status, String code, String message) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(status.value());
        ApiErrorResponse error = new ApiErrorResponse(
                code,
                message,
                LocalDateTime.now().toString()
        );
        new ObjectMapper().writeValue(response.getOutputStream(), error);
    }
}