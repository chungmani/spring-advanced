package org.example.expert.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.example.expert.domain.common.exception.ServerException;
import org.example.expert.domain.user.enums.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.Key;
import java.util.Base64;
import java.util.Date;

@Slf4j(topic = "JwtUtil")
@Component
public class JwtUtil {

    // Authorization 헤더에서 Bearer 인증 방식임을 나타내는 접두사이다. JWT 자체의 일부는 아니다.
    // 클라이언트는 "Bearer <JWT>"로 보내고, 서버는 접두사를 제거한 JWT를 검증한다.
    private static final String BEARER_PREFIX = "Bearer ";
    // 발급 시각부터 토큰이 유효한 기간을 밀리초 단위로 설정한다.
    private static final long TOKEN_TIME = 60 * 60 * 1000L; // 60분

    // Spring 설정의 jwt.secret.key 값을 주입받는다. 이 코드에서는 Base64로 인코딩된 키를 기대한다.
    @Value("${jwt.secret.key}")
    private String secretKey;
    // JWT 서명 생성과 검증에 사용할 키 객체이다. init()에서 준비한다.
    private Key key;
    // 사용할 서명 알고리즘으로 HS256(HMAC-SHA-256)을 선택한다.
    private final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;

    // 이 Bean의 의존성과 @Value 값이 주입된 후 실행하는 초기화 메서드이다.
    // 애플리케이션 전체에서 가장 먼저 실행된다는 의미는 아니다.
    @PostConstruct
    public void init() {
        // Base64 문자열을 원래의 키 바이트 배열로 디코딩한다. Base64는 64비트를 뜻하지 않는다.
        byte[] bytes = Base64.getDecoder().decode(secretKey);
        // 키 바이트로 HMAC 서명 생성과 검증에 사용할 비밀키 객체를 만든다.
        key = Keys.hmacShaKeyFor(bytes);
    }

    // 사용자 ID, 이메일, 역할을 담은 JWT를 만들고 "Bearer "를 붙여 반환한다.
    public String createToken(Long userId, String email, UserRole userRole) {
        // java.util.Date는 Java 표준 클래스이다. 현재 시각을 발급·만료 시각 계산에 사용한다.
        Date date = new Date();

        // 서명은 토큰의 위변조를 검증하기 위한 것이다. payload 내용을 암호화해서 숨기지는 않는다.
        return BEARER_PREFIX +
                Jwts.builder()
                        // 표준 claim인 sub(subject)에 사용자 ID를 문자열로 저장한다.
                        .setSubject(String.valueOf(userId))
                        // 사용자 정의 claim으로 이메일과 역할을 저장한다.
                        .claim("email", email)
                        .claim("userRole", userRole)
                        // 표준 claim인 exp에 만료 시각(현재 시각 + 60분)을 설정한다.
                        .setExpiration(new Date(date.getTime() + TOKEN_TIME))
                        .setIssuedAt(date) // 표준 claim인 iat에 발급 시각을 설정한다.
                        .signWith(key, signatureAlgorithm) // 서명에 사용할 비밀키와 알고리즘을 지정한다.
                        // 설정한 정보로 서명된 header.payload.signature 형태의 JWT 문자열을 생성한다.
                        .compact();
    }

    // "Bearer " 접두사(공백 포함 7자)를 제거하고 JWT 문자열만 반환한다.
    public String substringToken(String tokenValue) {
        // hasText는 null, 빈 문자열, 공백만 있는 문자열이면 false를 반환한다.
        // &&는 왼쪽이 true일 때만 오른쪽을 평가하므로 null에 startsWith()를 호출하지 않는다.
        if (StringUtils.hasText(tokenValue) && tokenValue.startsWith(BEARER_PREFIX)) {
            return tokenValue.substring(7);
        }
        // 값이 없거나 "Bearer "로 시작하지 않으면 현재 코드에서는 이 예외를 던진다.
        throw new ServerException("Not Found Token");
    }

    // "Bearer "가 제거된 JWT를 검증하고 payload의 Claims 전체를 반환한다.
    // 반환된 Claims에서 getSubject(), get("email") 등으로 필요한 값을 개별적으로 꺼낼 수 있다.
    public Claims extractClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key) // 서명을 검증할 비밀키를 지정한다.
                .build() // 위 설정을 사용하는 파서를 생성한다.
                .parseClaimsJws(token) // 서명과 만료 여부 등을 검증한다. 실패하면 예외가 발생한다.
                .getBody(); // 검증된 토큰의 payload를 Claims 객체로 얻는다.
    }
}
