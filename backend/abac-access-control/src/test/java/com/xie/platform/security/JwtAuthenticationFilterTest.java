package com.xie.platform.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xie.platform.utils.CurrentUserContext;
import com.xie.platform.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Test
    void doFilter_shouldRejectChangePasswordTokenOnProtectedPath() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtil, new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/project/list");
        request.addHeader("Authorization", "Bearer temp-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        Claims claims = new DefaultClaims();
        claims.setSubject("7");

        when(jwtUtil.parseToken("temp-token")).thenReturn(claims);
        when(jwtUtil.isChangePasswordToken(claims)).thenReturn(true);

        filter.doFilter(request, response, filterChain);

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("临时凭证仅可用于修改密码"));
        assertNull(request.getAttribute(CurrentUserContext.EMPLOYEE_ID_ATTR));
    }

    @Test
    void doFilter_shouldAllowRegularAccessTokenOnProtectedPath() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtil, new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/project/list");
        request.addHeader("Authorization", "Bearer access-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        Claims claims = new DefaultClaims();
        claims.setSubject("9");

        when(jwtUtil.parseToken("access-token")).thenReturn(claims);
        when(jwtUtil.isChangePasswordToken(claims)).thenReturn(false);

        filter.doFilter(request, response, filterChain);

        assertEquals(200, response.getStatus());
        assertEquals(9L, request.getAttribute(CurrentUserContext.EMPLOYEE_ID_ATTR));
        verify(jwtUtil).parseToken("access-token");
    }

    @Test
    void shouldNotFilter_shouldBypassChangePasswordEndpoint() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtil, new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/employee/change-password");

        assertTrue(filter.shouldNotFilter(request));
    }

    @Test
    void shouldNotFilter_shouldProtectBusinessEndpoint() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtil, new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/project/list");

        assertFalse(filter.shouldNotFilter(request));
    }
}
