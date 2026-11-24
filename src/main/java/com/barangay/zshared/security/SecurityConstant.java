package com.barangay.zshared.security;

/**
 * Holds all the constant values used across the security layer.
 * Keeps role names, endpoint arrays, and CORS settings in one place
 * so they are easy to find and update.
 */
public class SecurityConstant {

        // JWT header/token constants
        public static final String AUTHORIZATION_HEADER = "Authorization";
        public static final String BEARER_TOKEN_PREFIX = "Bearer ";

    // Role name constants used in security rules and authority checks
    public static final String ROLE_ROOT_ADMIN   = "ROOT_ADMIN";
    public static final String ROLE_SYSTEM_ADMIN = "SYSTEM_ADMIN";
    public static final String ROLE_ADMIN        = "ADMIN";
    public static final String ROLE_STAFF        = "STAFF";
    public static final String ROLE_OFFICER      = "OFFICER";

    // Endpoints that anyone can access without a token
    public static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/**",
            "/api/v1/cms/announcements/public",
            "/api/v1/cms/services/public",
            "/api/public/**",
            "/uploads/**",
            "/error",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/v3/api-docs.yaml"
    };

    // Endpoints that only ROOT_ADMIN can access
    public static final String[] ROOT_ADMIN_ENDPOINTS = {
            "/api/v1/dashboard/**",
            "/api/v1/audit/**",
            "/api/v1/audit-logs/**"
    };

    // Endpoints that ADMIN and ROOT_ADMIN (SYSTEM_ADMIN) can access
    public static final String[] ADMIN_ENDPOINTS = {
            "/api/v1/users/**",
            "/api/v1/user-management/stats",
            "/api/v1/system-users/**",
            "/api/v1/admin/**",
            "/api/v1/departments/**",
            "/api/v1/roles/**",
            "/api/settings/**",
            "/api/officials/**",
            "/api/v1/cms/**"
    };

    // Endpoints restricted to ROOT_ADMIN or staff assigned to the VAWC department
    public static final String[] VAWC_ENDPOINTS = {
            "/api/v1/vawc/**",
            "/api/v1/bpo/**"
    };

    // Endpoints restricted to ROOT_ADMIN or staff assigned to the LUPON department
    public static final String[] LUPON_ENDPOINTS = {
            "/api/v1/lupon/**",
            "/api/v1/sessions/**",
            "/api/v1/lupon-extensions/**"
    };

    // Endpoints restricted to ROOT_ADMIN or staff assigned to the BLOTTER department
    public static final String[] BLOTTER_ENDPOINTS = {
            "/api/v1/blotter",
            "/api/v1/blotter/**",
    };

    // Endpoints restricted to ROOT_ADMIN or staff assigned to the BCPC department
    public static final String[] BCPC_ENDPOINTS = {
            "/api/v1/bcpc/**"
    };

    // Endpoints restricted to ROOT_ADMIN or staff assigned to the CLEARANCE department
    public static final String[] CLEARANCE_ENDPOINTS = {
            "/api/v1/clearance/**",
            "/api/v1/permits/**",
            "/api/v1/ftjs/**"
    };



    // Endpoints accessible by any logged in staff regardless of department
    public static final String[] SHARED_CASE_ENDPOINTS = {
            "/api/v1/cases/**",
            "/api/v1/cases/shared/**",
            "/api/v1/hearings/**",
            "/api/v1/notices/**",
    };

    // CORS configuration settings (origins are dynamically managed via application.properties / .env)
    public static final String[] ALLOWED_METHODS = {
            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
    };

    public static final String[] ALLOWED_HEADERS = {
            "Authorization", "Content-Type", "X-Requested-With", "Accept"
    };

    public static final String[] EXPOSED_HEADERS = {
            "Content-Disposition", "Content-Type", "Content-Length"
    };
}
