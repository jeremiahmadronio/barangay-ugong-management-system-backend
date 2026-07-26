package com.barangay.zshared.security;
/**
 * Holds all the constant values used across the security layer.
 * Keeps role names, endpoint arrays, and CORS settings in one place
 * so they are easy to find and update.
 */
public class SecurityConstant {

    // Role name constants used in security rules and authority checks
    public static final String ROLE_ROOT_ADMIN   = "ROOT_ADMIN";
    public static final String ROLE_SYSTEM_ADMIN = "SYSTEM_ADMIN";
    public static final String ROLE_ADMIN        = "ADMIN";
    public static final String ROLE_STAFF        = "STAFF";

    // Endpoints that anyone can access without a token
    public static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/**",
            "/api/v1/departments/**",
            "/api/v1/permissions/**",
            "/api/v1/persons/**",
            "/error"
    };
    // Endpoints that only ROOT_ADMIN can access
    public static final String[] ROOT_ADMIN_ENDPOINTS = {
            "/api/v1/dashboard/**",
            "/api/v1/audit/**"
    };

    // Endpoints that ADMIN and ROOT_ADMIN (SYSTEM_ADMIN) can access
    public static final String[] ADMIN_ENDPOINTS = {
            "/api/v1/users/**",
            "/api/v1/user-management/stats",
            "/api/v1/departments/**",
            "/api/v1/roles/**"
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
            "/api/v1/blotter/**"
    };

    // Endpoints restricted to ROOT_ADMIN or staff assigned to the BCPC department
    public static final String[] BCPC_ENDPOINTS = {
            "/api/v1/bcpc/**"
    };

    // Endpoints restricted to ROOT_ADMIN or staff assigned to the CLEARANCE department
    public static final String[] CLEARANCE_ENDPOINTS = {
            "/api/v1/clearance/**"
    };

    // Endpoints restricted to ROOT_ADMIN or staff assigned to the FTJS department
    public static final String[] FTJS_ENDPOINTS = {
            "/api/v1/ftjs/**"
    };

    // Endpoints that any authenticated user can access regardless of department
    public static final String[] SHARED_CASE_ENDPOINTS = {
            "/api/v1/cases/**",
            "/api/v1/residents/**",
            "/api/v1/persons/**",
            "/api/v1/ftjs/**",
            "/api/v1/certificates/**"
    };

    // Allowed frontend origins for cross-origin requests
    public static final String[] ALLOWED_ORIGINS = {
            "http://localhost:5173",
            "http://localhost:5174",
            "http://localhost:5175",
            "http://localhost:5176",
            "http://localhost:3000",
            "https://baranggay-management.vercel.app"
    };

    // Allowed HTTP methods for cross-origin requests
    public static final String[] ALLOWED_METHODS =
            {"GET","POST","PUT","DELETE","PATCH","OPTIONS"};

    // Allowed request headers for cross-origin requests
    public static final String[] ALLOWED_HEADERS = {"*"};

    // The prefix expected before the JWT token in the Authorization header
    public static final String TOKEN_PREFIX  = "Bearer ";

    // The name of the HTTP header that carries the JWT token
    public static final String HEADER_STRING = "Authorization";

    // Prevent instantiation — this class is a constants holder only
    private SecurityConstant() { throw new UnsupportedOperationException(); }
}