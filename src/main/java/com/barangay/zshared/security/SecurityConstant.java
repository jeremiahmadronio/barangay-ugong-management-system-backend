package com.barangay.zshared.security;

public class SecurityConstant {

    // ── Roles ────────────────────────────────────────────────────────
    public static final String ROLE_ROOT_ADMIN = "ROOT_ADMIN";
    public static final String ROLE_ADMIN      = "ADMIN";
    public static final String ROLE_STAFF      = "STAFF";

    // ── Public (no auth needed) ───────────────────────────────────────
    public static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/**",
            "/error"
    };

    // ── ROOT_ADMIN only ───────────────────────────────────────────────
    public static final String[] ROOT_ADMIN_ENDPOINTS = {
            "/api/v1/dashboard/**",
            "/api/v1/users/create-admin",
            "/api/v1/users/update-admin",
            "/api/v1/departments/**",
            "/api/v1/roles/**",
            "/api/v1/audit/**"
    };

    // ── Any authenticated ADMIN or higher ────────────────────────────
    public static final String[] ADMIN_ENDPOINTS = {
            "/api/v1/user-management/stats",
            "/api/v1/users/{userId}/lock",
            "/api/v1/users/update-status"
    };

    // ── Department-specific (enforced via WebExpressionAuthorizationManager) ─
    public static final String[] VAWC_ENDPOINTS = {
            "/api/v1/vawc/**",
            "/api/v1/bpo/**"
    };

    public static final String[] LUPON_ENDPOINTS = {
            "/api/v1/lupon/**",
            "/api/v1/sessions/**",
            "/api/v1/lupon-extensions/**"
    };

    public static final String[] BLOTTER_ENDPOINTS = {
            "/api/v1/blotter/**"
    };

    public static final String[] BCPC_ENDPOINTS = {
            "/api/v1/bcpc/**"
    };

    // ── Shared case/resident endpoints (any dept staff) ──────────────
    public static final String[] SHARED_CASE_ENDPOINTS = {
            "/api/v1/cases/**",
            "/api/v1/residents/**",
            "/api/v1/persons/**",
            "/api/v1/ftjs/**",
            "/api/v1/certificates/**"
    };

    // ── CORS ─────────────────────────────────────────────────────────
    public static final String[] ALLOWED_ORIGINS = {
            "http://localhost:5173",
            "http://localhost:3000",
            "https://baranggay-management.vercel.app"
    };
    public static final String[] ALLOWED_METHODS =
            {"GET","POST","PUT","DELETE","PATCH","OPTIONS"};
    public static final String[] ALLOWED_HEADERS = {"*"};

    public static final String TOKEN_PREFIX  = "Bearer ";
    public static final String HEADER_STRING = "Authorization";

    private SecurityConstant() { throw new UnsupportedOperationException(); }
}