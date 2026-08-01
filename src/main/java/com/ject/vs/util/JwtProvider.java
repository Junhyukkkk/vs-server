package com.ject.vs.util;

import com.ject.vs.auth.domain.TokenStatus;
import com.ject.vs.auth.domain.TokenType;
import com.ject.vs.auth.exception.TokenErrorCode;
import com.ject.vs.auth.port.in.dto.TokenInfo;
import com.ject.vs.common.exception.BusinessException;
import com.ject.vs.config.JwtProperties;
import com.ject.vs.user.domain.User;
import com.ject.vs.user.domain.UserRepository;
import com.ject.vs.user.exception.UserErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtProvider {
    private final UserRepository userRepository;
    private final JwtProperties jwtProperties;
    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(decodeSecret(jwtProperties.secret()));
    }

    /**
     * APP_JWT_SECRET은 base64로 인코딩된 값을 기대한다.
     *
     * <p>다만 값이 GitHub Actions → SSH → 컨테이너 환경변수를 거치는 동안
     * 줄바꿈이 공백으로 바뀌어 섞여 들어올 수 있고, 생성 방식에 따라
     * URL-safe base64({@code -}, {@code _})로 전달되기도 한다.
     * 둘 다 표준 base64와 동일한 바이트열을 의미하므로 정규화한 뒤 디코딩한다.
     */
    private byte[] decodeSecret(String raw) {
        String normalized = raw.replaceAll("\\s", "")
                .replace('-', '+')
                .replace('_', '/');
        try {
            return Decoders.BASE64.decode(normalized);
        } catch (DecodingException e) {
            throw new IllegalStateException(
                    "APP_JWT_SECRET이 base64 형식이 아닙니다. "
                            + "openssl rand -base64 64 | tr -d '\\n' 으로 생성한 값을 사용하세요.", e);
        }
    }

    public TokenInfo createAccessToken(Long userId) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(jwtProperties.accessTokenExpirationSeconds());

        String token = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("type", TokenType.ACCESS.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey)
                .compact();

        return new TokenInfo(token, TokenType.ACCESS, expiresAt, userId);
    }

    public TokenInfo createRefreshToken(Long userId) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(jwtProperties.refreshTokenExpirationSeconds());

        String token = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("type", TokenType.REFRESH.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey)
                .compact();

        return new TokenInfo(token, TokenType.REFRESH, expiresAt, userId);
    }

    public TokenStatus validationToken(String token) {
        if(token == null || token.isBlank()) return TokenStatus.EMPTY;
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return TokenStatus.VALID;
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            return TokenStatus.EXPIRED;
        } catch (JwtException | IllegalArgumentException e) {
            return TokenStatus.INVALID;
        }
    }

    public Claims getClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            throw new BusinessException(TokenErrorCode.EXPIRED_TOKEN);
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
            throw new BusinessException(TokenErrorCode.INVALID_TOKEN);
        }
    }

    public Long getUserId(String token) {
        return Long.parseLong(getClaims(token).getSubject());
    }

    public User getUser(String token) {
        return userRepository.findById(getUserId(token)).orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
    }

    public String getTokenType(String token) {
        Object type = getClaims(token).get("type");
        return type == null ? null : type.toString();
    }

    public TokenInfo parseToken(String token) {
        Claims claims = getClaims(token);
        String type = claims.get("type").toString();
        Long userId = Long.parseLong(claims.getSubject());
        Instant expiresAt = claims.getExpiration().toInstant();
        return new TokenInfo(token, TokenType.valueOf(type), expiresAt, userId);
    }
}
