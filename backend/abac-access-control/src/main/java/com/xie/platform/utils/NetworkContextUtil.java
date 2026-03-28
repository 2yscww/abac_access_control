package com.xie.platform.utils;

import com.xie.platform.model.enumValue.NetworkZone;
import jakarta.servlet.http.HttpServletRequest;

import java.util.regex.Pattern;

public final class NetworkContextUtil {

    private static final Pattern PRIVATE_172_RANGE =
            Pattern.compile("^172\\.(1[6-9]|2\\d|3[0-1])\\..*");

    private NetworkContextUtil() {
    }

    public static String extractClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        String ip = request.getHeader("X-Forwarded-For");
        if (isUnknown(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (isUnknown(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return isUnknown(ip) ? "unknown" : ip.trim();
    }

    public static NetworkZone resolveNetworkZone(String ipAddress) {
        if (isUnknown(ipAddress)) {
            return NetworkZone.UNKNOWN;
        }

        String ip = ipAddress.trim();
        if (isLoopback(ip)) {
            return NetworkZone.LOOPBACK;
        }
        if (isInternal(ip)) {
            return NetworkZone.INTERNAL;
        }
        return NetworkZone.PUBLIC;
    }

    private static boolean isUnknown(String value) {
        return value == null || value.isBlank() || "unknown".equalsIgnoreCase(value.trim());
    }

    private static boolean isLoopback(String ip) {
        return "127.0.0.1".equals(ip)
                || "::1".equals(ip)
                || "0:0:0:0:0:0:0:1".equals(ip);
    }

    private static boolean isInternal(String ip) {
        return ip.startsWith("10.")
                || ip.startsWith("192.168.")
                || PRIVATE_172_RANGE.matcher(ip).matches()
                || ip.startsWith("fc")
                || ip.startsWith("fd");
    }
}
