package org.example.expert.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.expert.domain.user.enums.UserRole;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j // log.info(), log.warn() 등을 사용할 수 있도록 로거 필드를 생성한다.
@RequiredArgsConstructor // 초기화되지 않은 final 필드 등을 인자로 받는 생성자를 생성한다.
public class JwtFilter implements Filter { // Servlet의 Filter 인터페이스를 구현한다.

    // JWT 생성·검증 도구이다. 이 필터에서는 접두사 제거와 JWT 검증·Claims 추출에 사용한다.
    private final JwtUtil jwtUtil;
    // Java 객체와 JSON 사이의 변환 도구이다. 여기서는 오류 정보 Map을 JSON 문자열로 변환한다.
    private final ObjectMapper objectMapper;

    // FilterConfig가 new JwtFilter(jwtUtil, objectMapper)로 위 두 객체를 생성자에 전달해 등록한다.
    // Filter 인터페이스의 doFilter 메서드를 재정의한다. 인자 타입과 순서는 인터페이스 규약을 따른다.
    // 인자 이름은 바꿀 수 있으며, 실제 인자 객체는 Servlet 컨테이너가 호출 시 전달한다.
    // FilterChain은 현재 필터 다음의 필터와 최종 Servlet으로 처리를 이어 주는 객체이다.
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        // 전달받은 요청 객체를 HTTP 전용 타입으로 형변환하여 헤더·URI 등의 HTTP 기능을 사용한다.
        // 새 요청을 보내거나 새 객체를 만드는 동작은 아니다.
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        // 응답 객체도 HTTP 전용 타입으로 형변환한다. 이후 상태 코드·응답 헤더 등을 설정할 수 있다.
        // 형변환하는 이 줄 자체가 클라이언트에게 응답을 전송하지는 않는다.
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 요청 URI의 경로를 읽는다. 쿼리 문자열(? 뒤의 값)은 포함하지 않는다.
        String url = httpRequest.getRequestURI();

        // /auth로 시작하는 요청은 이 필터의 JWT 검사를 생략하고 다음 처리로 넘긴다.
        // chain.doFilter()는 다음 필터를 호출하고, 필터가 더 없으면 Servlet으로 이어진다.
        // 다음 처리가 끝나 돌아오면 return으로 현재 메서드를 종료한다.
        if (url.startsWith("/auth")) {
            chain.doFilter(request, response);
            return;
        }

        // 클라이언트가 보낸 Authorization 헤더 값을 읽는다. 예: "Bearer eyJ..."
        String bearerJwt = httpRequest.getHeader("Authorization");

        // Authorization 헤더가 없으면 401 응답을 작성하고 다음 처리로 넘기지 않는다.
        if (bearerJwt == null) {
            log.warn("인증 헤더 누락: URI={}", url);
            sendErrorResponse(httpResponse, HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
            return;
        }

        // 헤더 값 앞의 "Bearer " 접두사를 제거하고 JWT 문자열만 얻는다.
        // 이 호출은 try 밖에 있으므로 여기서 발생한 예외는 아래 catch에서 처리하지 못한다.
        String jwt = jwtUtil.substringToken(bearerJwt);

        try {
            // JWT의 서명과 만료 여부 등을 검증하고 payload의 정보(Claims)를 얻는다.
            // 이 프로젝트의 Claims에는 사용자 ID(subject), 이메일, 역할, 발급·만료 시각이 들어 있다.
            Claims claims = jwtUtil.extractClaims(jwt);
            if (claims == null) {
                log.warn("Claims 추출 실패: URI={}", url);
                sendErrorResponse(httpResponse, HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
                return;
            }

            // 같은 요청의 Interceptor나 ArgumentResolver에서 읽을 수 있도록 request attribute에 저장한다.
            // userId는 Long, email과 userRole은 JWT에서 꺼낸 문자열이며 SecurityContext에 저장하는 것은 아니다.
            httpRequest.setAttribute("userId", Long.parseLong(claims.getSubject()));
            httpRequest.setAttribute("email", claims.get("email"));
            httpRequest.setAttribute("userRole", claims.get("userRole"));

            // 검사에 통과하면 request와 response 객체를 다음 필터 또는 Servlet으로 전달한다.
            // 다음 처리가 끝나면 이 지점으로 돌아온다. 응답 본문 자체를 인자로 넘기는 것은 아니다.
            chain.doFilter(request, response);
        } catch (ExpiredJwtException e) {
            log.info("JWT 만료: userId={}, URI={}", e.getClaims().getSubject(), url);
            sendErrorResponse(httpResponse, HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        } catch (SecurityException | MalformedJwtException | UnsupportedJwtException e) {
            log.error("JWT 검증 실패 [{}]: URI={}", e.getClass().getSimpleName(), url, e);
            sendErrorResponse(httpResponse, HttpStatus.BAD_REQUEST, "인증이 필요합니다.");
        } catch (Exception e) {
            // 위 try 안에서 발생해 여기까지 전파된 나머지 예외를 처리한다.
            // chain.doFilter()도 try 안에 있으므로 다음 처리 단계에서 전파된 예외도 포함될 수 있다.
            log.error("예상치 못한 오류: URI={}", url, e);
            sendErrorResponse(httpResponse, HttpStatus.INTERNAL_SERVER_ERROR, "요청 처리 중 오류가 발생했습니다.");
        }
    }

    private void sendErrorResponse(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        // HTTP 상태 코드와 JSON 응답 형식을 설정한다.
        response.setStatus(status.value());
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", status.name());
        errorResponse.put("code", status.value());
        errorResponse.put("message", message);

        // Map을 JSON 문자열로 변환하여 응답 본문에 쓴다. 예외를 던지는 메서드는 아니다.
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
