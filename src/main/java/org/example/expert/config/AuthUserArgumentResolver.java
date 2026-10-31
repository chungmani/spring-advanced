package org.example.expert.config;

import jakarta.servlet.http.HttpServletRequest;
import org.example.expert.domain.auth.exception.AuthException;
import org.example.expert.domain.common.annotation.Auth;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.user.enums.UserRole;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

// Spring Bean으로 등록한다. MVC에서 사용하려면 WebMvcConfig에도 ArgumentResolver로 등록해야 한다.
@Component
// Controller의 @Auth AuthUser 파라미터에 전달할 실제 인자 객체를 만드는 클래스이다.
// JWT 검증은 앞선 JwtFilter가 담당하고, 여기서는 request에 저장된 사용자 정보를 꺼낸다.
public class AuthUserArgumentResolver implements HandlerMethodArgumentResolver {

    // Spring MVC가 Controller 인자를 준비할 때, 이 Resolver가 해당 파라미터를 처리할 수 있는지 확인한다.
    // MethodParameter는 Controller 메서드의 특정 파라미터에 대한 타입·어노테이션 등의 정보를 담는다.
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        // 파라미터에 @Auth가 있으면 어노테이션 객체를 얻고, 없으면 null이므로 false가 된다.
        boolean hasAuthAnnotation = parameter.getParameterAnnotation(Auth.class) != null;
        // 파라미터의 선언 타입이 정확히 AuthUser인지 확인한다.
        boolean isAuthUserType = parameter.getParameterType().equals(AuthUser.class);

        // 두 boolean이 다르면 하나만 사용한 경우이다. 이 프로젝트에서는 잘못된 선언으로 보고 예외를 던진다.
        // 예: @Auth String value 또는 @Auth 없는 AuthUser 파라미터
        if (hasAuthAnnotation != isAuthUserType) {
            throw new AuthException("@Auth와 AuthUser 타입은 함께 사용되어야 합니다.");
        }

        // 어노테이션 자체가 아니라 boolean을 반환한다.
        // 둘 다 있으면 true(처리 가능), 둘 다 없으면 false(다른 Resolver가 처리하도록 함)이다.
        return hasAuthAnnotation;
    }

    // 이 Resolver가 담당하는 파라미터에 넣을 실제 값을 생성한다. 반환값이 Controller의 인자로 전달된다.
    @Override
    public Object resolveArgument(
            // 처리할 Controller 파라미터의 정보이다. @Nullable은 null을 허용한다는 표시이다.
            @Nullable MethodParameter parameter,
            // MVC의 모델·뷰 정보를 관리하는 객체이다. 이 구현에서는 사용하지 않는다.
            @Nullable ModelAndViewContainer mavContainer,
            // Spring이 원래 요청 객체를 감싸서 제공하는 객체이다.
            NativeWebRequest webRequest,
            // 요청 값을 객체에 바인딩하는 WebDataBinder를 만드는 도구이다. 여기서는 사용하지 않는다.
            @Nullable WebDataBinderFactory binderFactory
    ) {
        // Spring의 래퍼에서 원래 Servlet 요청을 꺼내 HTTP 요청 타입으로 형변환한다.
        // JwtFilter에서 attribute를 저장한 요청 객체를 여기서 읽는 것이다.
        HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();

        // getAttribute()의 반환 타입은 Object이므로 저장된 실제 타입인 Long과 String으로 형변환한다.
        Long userId = (Long) request.getAttribute("userId");
        String email = (String) request.getAttribute("email");
        // request에 저장된 역할 문자열을 UserRole enum으로 변환한다.
        UserRole userRole = UserRole.of((String) request.getAttribute("userRole"));

        // 세 정보를 묶은 AuthUser를 만들어 Controller의 @Auth AuthUser 인자로 전달한다.
        return new AuthUser(userId, email, userRole);
    }
}
