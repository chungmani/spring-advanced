package org.example.expert.config;

import org.example.expert.domain.auth.exception.AuthException;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.common.exception.ServerException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

// 여러 Controller에 공통으로 적용할 예외 처리 클래스로 등록한다.
// Spring MVC로 전달된 예외 중 아래 @ExceptionHandler에 맞는 예외를 처리하고 반환값을 응답 본문에 쓴다.
// Service의 예외도 Controller 호출을 거쳐 MVC로 전파되면 처리 대상이 된다.
// MVC 바깥의 JwtFilter 자체에서 발생한 예외까지 자동으로 처리하는 것은 아니다.
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 이 타입(또는 하위 타입)의 예외를 처리할 메서드임을 지정한다. ex에는 실제 발생한 예외가 전달된다.
    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<Map<String, Object>> invalidRequestExceptionException(InvalidRequestException ex) {
        // 현재 프로젝트에서는 잘못된 요청 예외를 HTTP 400 응답으로 바꾼다.
        HttpStatus status = HttpStatus.BAD_REQUEST;
        // 예외에 담긴 메시지를 꺼내 공통 응답 생성 메서드에 전달한다.
        return getErrorResponse(status, ex.getMessage());
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<Map<String, Object>> handleAuthException(AuthException ex) {
        // 현재 프로젝트에서는 인증 관련 예외를 HTTP 401 응답으로 바꾼다.
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        return getErrorResponse(status, ex.getMessage());
    }

    @ExceptionHandler(ServerException.class)
    public ResponseEntity<Map<String, Object>> handleServerException(ServerException ex) {
        // 현재 프로젝트에서는 서버 오류 예외를 HTTP 500 응답으로 바꾼다.
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        return getErrorResponse(status, ex.getMessage());
    }

    // 위 세 핸들러가 공통으로 사용하는 응답 생성 메서드이다. 자체가 예외 핸들러나 API는 아니다.
    // ResponseEntity는 HTTP 상태 코드·헤더·본문을 담는 객체이며, 여기서는 본문 타입이 Map이다.
    public ResponseEntity<Map<String, Object>> getErrorResponse(HttpStatus status, String message) {
        // 키는 String이고, 값은 문자열·숫자 등 여러 타입이므로 Object로 선언한다.
        Map<String, Object> errorResponse = new HashMap<>();
        // name(): 상태 이름 문자열(예: "BAD_REQUEST"), value(): HTTP 상태 코드 숫자(예: 400)
        errorResponse.put("status", status.name());
        errorResponse.put("code", status.value());
        errorResponse.put("message", message);

        // Map은 응답 본문, status는 실제 HTTP 응답 상태로 설정한다.
        // 반환된 Map은 Spring MVC의 메시지 컨버터를 통해 일반적인 JSON 응답으로 변환된다.
        return new ResponseEntity<>(errorResponse, status);
    }
}

