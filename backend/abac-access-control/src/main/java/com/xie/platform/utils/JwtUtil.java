package com.xie.platform.utils;

import java.nio.charset.StandardCharsets;
import java.util.Date;



import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import com.xie.platform.access.subject.Subject;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;


@Component
public class JwtUtil {
    @Value("${security.jwt.secret}")
    private String secret;

    @Value("${security.jwt.expire}")
    private long expire;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 JWT（登录成功后调用）
     */
    public String generateToken(Subject subject) {
        return Jwts.builder()
                .setSubject(subject.getEmployeeId().toString())
                .claim("deptId", subject.getDeptId())
                .claim("level", subject.getLevel())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expire))
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析 JWT（请求进入时调用）
     * 如果 token 非法 / 过期，会直接抛异常
     */
    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
