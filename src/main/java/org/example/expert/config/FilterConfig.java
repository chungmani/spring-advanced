package org.example.expert.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Spring이 설정 클래스로 인식한다. 이 클래스도 Bean으로 관리하며, 내부의 @Bean 정의를 처리한다.
@Configuration
// 두 final 필드를 인자로 받는 생성자를 만든다. Spring은 이 생성자로 기존 Bean들을 주입한다.
@RequiredArgsConstructor
public class FilterConfig {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    // 메서드가 반환하는 FilterRegistrationBean 객체를 Spring Bean으로 등록한다.
    // 기본 Bean 이름은 메서드명인 jwtFilter이다.
    // Spring Boot는 이 등록 정보를 사용해 Servlet 컨테이너에 실제 필터를 등록한다.
    @Bean
    public FilterRegistrationBean<JwtFilter> jwtFilter() {
        // 적용할 필터와 URL 패턴 등을 담을 등록 정보 객체를 생성한다.
        FilterRegistrationBean<JwtFilter> registrationBean = new FilterRegistrationBean<>();
        // JwtFilter를 직접 생성하고, 등록 정보 객체에 사용할 필터로 지정한다.
        // @Bean이 직접 등록하는 대상은 위의 등록 정보 객체이며, 이 JwtFilter 자체를 별도 Bean으로 등록하는 것은 아니다.
        registrationBean.setFilter(new JwtFilter(jwtUtil, objectMapper));
        // 애플리케이션의 모든 요청 경로에 적용한다. /auth 검사 생략 여부는 JwtFilter 내부에서 결정한다.
        registrationBean.addUrlPatterns("/*");

        // 완성한 등록 정보 객체를 반환하여 Spring이 관리하도록 한다.
        return registrationBean;
    }
}
