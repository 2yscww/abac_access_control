package com.xie.platform.security;

import com.xie.platform.exception.BizException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

public final class CurrentUserContext {

    public static final String EMPLOYEE_ID_ATTR = CurrentUserContext.class.getName() + ".employeeId";

    private CurrentUserContext() {
    }

    public static Long getEmployeeId() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            Object value = attributes.getAttribute(EMPLOYEE_ID_ATTR, RequestAttributes.SCOPE_REQUEST);
            if (value instanceof Long employeeId) {
                return employeeId;
            }
            if (value instanceof String employeeId) {
                return Long.parseLong(employeeId);
            }
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Long employeeId) {
            return employeeId;
        }
        if (principal instanceof String employeeId
                && !employeeId.isBlank()
                && !"anonymousUser".equals(employeeId)) {
            return Long.parseLong(employeeId);
        }

        return null;
    }

    public static Long getRequiredEmployeeId() {
        Long employeeId = getEmployeeId();
        if (employeeId == null) {
            throw new BizException("未登录或登录已过期");
        }
        return employeeId;
    }
}
