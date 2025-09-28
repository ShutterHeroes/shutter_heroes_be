package com.example.demo.config.security.jwt;

import com.example.demo.domain.enums.JwtType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtUtil {
    @Value("${jwt.access-token.expiration}")
    private long ACCESS_TOKEN_EXPIRATION_TIME;
    @Value("${jwt.secretKey}")
    private String JWT_SECRET_KEY;

    private static final SignatureAlgorithm SIGNATURE_ALGORITHM = SignatureAlgorithm.HS256;
    private static final String USERNAME_KEY = "username";
    private static final String CODE_KEY = "code"; // 토큰의 세트를 구분하기 위한 값 (하나의 refreshToken은 하나 이상의 accessToken과 연결)

    /**
     * jwt를 생성하여 반환하는 메서드
     *
     * @param username 유저의 email
     * @param code     토큰의 세트를 구분하기 위한 값. 여러 기기에서의 동시 로그인을 위함 (하나의 refreshToken은 하나 이상의 accessToken과 연결)
     * @param jwtType  jwt 토큰 유형
     * @return 유저의 jwt
     */
    public String generateJwt(String username, String code, JwtType jwtType) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(USERNAME_KEY, username);
        claims.put(CODE_KEY, code);
        return buildJwt(
            claims,
            jwtType.isAccessToken() ? ACCESS_TOKEN_EXPIRATION_TIME : (ACCESS_TOKEN_EXPIRATION_TIME * 30)
        );
    }

    /**
     * 테스트용 만료된 jwt를 생성하여 반환하는 메서드
     * ⚠️ 테스트 환경에서만 사용 - 프로덕션에서는 사용하지 말 것
     *
     * @param username 유저의 email
     * @param code     토큰의 세트를 구분하기 위한 값
     * @return 만료된 jwt
     */
    @org.springframework.context.annotation.Profile({"test", "local"})
    public String generateExpiredJwt(String username, String code) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(USERNAME_KEY, username);
        claims.put(CODE_KEY, code);
        return buildExpiredJwt(claims);
    }

    /**
     * jwt claims에서 username을 추출하는 메서드
     * <ul>
     *     <li>기간이 만료된 경우, throw ExpiredJwtException</li>
     * </ul>
     *
     * @param token	jwt 토큰
     * @return		username
     */
    public String extractUsername(String token) {
        Claims claims = extractClaims(token);
        return (String) claims.get(USERNAME_KEY);
    }

    /**
     * jwt claims에서 code를 추출하는 메서드
     * <ul>
     *     <li>기간이 만료된 경우, throw ExpiredJwtException</li>
     * </ul>
     *
     * @param token	jwt 토큰
     * @return code
     */
    public String extractCode(String token) {
        Claims claims = extractClaims(token);
        return (String) claims.get(CODE_KEY);
    }

    /**
     * jwt를 생성하여 반환하는 메서드
     *
     * @param claims      	  jwt안에 들어가는 정보들
     * @param EXPIRATION_TIME jwt 유효 시간 (밀리초)
     * @return 유저의 jwt
     */
    private String buildJwt(Map<String, Object> claims, final long EXPIRATION_TIME) {
        return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(new Date(System.currentTimeMillis()))
            .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
            .signWith(getSigningKey(), SIGNATURE_ALGORITHM)
            .compact();
    }

    /**
     * 테스트용 만료된 jwt를 생성하여 반환하는 메서드
     * ⚠️ 테스트 환경에서만 사용
     *
     * @param claims jwt안에 들어가는 정보들
     * @return 만료된 jwt
     */
    private String buildExpiredJwt(Map<String, Object> claims) {
        long pastTime = System.currentTimeMillis() - 10000; // 10초 전
        return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(new Date(pastTime - 1000)) // 11초 전 발급
            .setExpiration(new Date(pastTime)) // 10초 전 만료
            .signWith(getSigningKey(), SIGNATURE_ALGORITHM)
            .compact();
    }

    /**
     * jwt를 파싱하여 claims를 반환하는 메서드.
     * 기간 만료, 잘못된 형식 등 예외가 발생할 시, 자동으로 JwtException를 상속한 예외들이 발생한다.
     *
     * @param token jwt 토큰
     * @return      jwt claims
     */
    private Claims extractClaims(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
    }

    /**
     * JWT_SECRET_KEY 값을 jwt 서명에 사용할 Key 객체로 변환하는 메서드
     *
     * @return jwt 서명에 사용할 Key 객체
     */
    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(JWT_SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
