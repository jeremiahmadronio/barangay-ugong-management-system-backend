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
import static org.ietf.jgss.GSSException.UNAUTHORIZED;
import static org.springframework.boot.actuate.autoconfigure.cloudfoundry.CloudFoundryAuthorizationException.Reason.ACCESS_DENIED;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // enables @PreAuthorize
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    private static AuthorizationManager<RequestAuthorizationContext> deptAccess(String dept) {
        return new WebExpressionAuthorizationManager(
                "hasRole('ROOT_ADMIN') or " +
                        "(hasAnyRole('ADMIN','STAFF') and hasAuthority('DEPT_" + dept + "'))"
        );
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers(ROOT_ADMIN_ENDPOINTS).hasRole(ROLE_ROOT_ADMIN)
                        .requestMatchers(ADMIN_ENDPOINTS).hasAnyRole(ROLE_ADMIN, ROLE_ROOT_ADMIN)
                        .requestMatchers(VAWC_ENDPOINTS).access(deptAccess("VAWC"))
                        .requestMatchers(LUPON_ENDPOINTS).access(deptAccess("LUPON"))
                        .requestMatchers(BLOTTER_ENDPOINTS).access(deptAccess("BLOTTER"))
                        .requestMatchers(BCPC_ENDPOINTS).access(deptAccess("BCPC"))
                        .requestMatchers(SHARED_CASE_ENDPOINTS).hasAnyRole(ROLE_ROOT_ADMIN, ROLE_ADMIN, ROLE_STAFF)
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


    //helper to write response error 
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