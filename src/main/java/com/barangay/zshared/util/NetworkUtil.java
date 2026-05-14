package com.barangay.zshared.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class NetworkUtil {

    private static final String[] HEADERS = {
            "X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR"
    };


    public static String getClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            return "0.0.0.0";
        }

        HttpServletRequest request = attributes.getRequest();

        for (String header : HEADERS) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.contains(",") ? ip.split(",")[0].trim() : ip;
            }
        }
        return request.getRemoteAddr();
    }
}