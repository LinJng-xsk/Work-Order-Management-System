// security/JwtUtils.java
package com.campus.woms.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

/** JWT 工具：签发 / 解析 / 黑名单（注销即时失效） */
@Component
@RequiredArgsConstructor
public class JwtUtils {

    private final StringRedisTemplate stringRedisTemplate;

    @Value("${woms.jwt.secret}")
    private String secret;

    @Value("${woms.jwt.expire-minutes:120}")
    private long expireMinutes;

    private SecretKey key;

    private static final String BLACKLIST_PREFIX = "woms:blacklist:";

    @PostConstruct
    public void init() {
        // HS256 要求密钥至少 32 字节
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** 签发 Token：jti 用于黑名单定位 */
    public String generateToken(Long userId, String username) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expireMinutes * 60_000);
        return Jwts.builder()
                .id(UUID.randomUUID().toString().replace("-", ""))
                .subject(String.valueOf(userId))
                .claim("username", username)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    /** 解析 Token，非法/过期抛 JwtException */
    public Claims parseToken(String token) throws JwtException {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    /** 注销：把 jti 加入黑名单，TTL = 剩余有效期 */
    public void blacklist(String jti, Date expiration) {
        long ttl = expiration.getTime() - System.currentTimeMillis();
        if (ttl > 0) {
            stringRedisTemplate.opsForValue().set(BLACKLIST_PREFIX + jti, "1", Duration.ofMillis(ttl));
        }
    }

    /** 是否已进入黑名单 */
    public boolean isBlacklisted(String jti) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(BLACKLIST_PREFIX + jti));
    }
}
