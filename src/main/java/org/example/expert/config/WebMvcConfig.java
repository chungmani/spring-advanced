package org.example.expert.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

// Spring 설정 클래스로 등록한다. 요청마다 거쳐 가는 단계가 아니라 MVC 동작을 미리 구성하는 클래스이다.
@Configuration
// final 필드를 인자로 받는 생성자를 만든다. Spring이 기존 AuthUserArgumentResolver Bean을 주입한다.
@RequiredArgsConstructor
// WebMvcConfigurer는 Spring MVC 설정을 추가·조정할 수 있는 메서드들을 제공하는 인터페이스이다.
// 필요한 메서드만 재정의하여 ArgumentResolver, Interceptor 등을 등록할 수 있다.
public class WebMvcConfig implements WebMvcConfigurer {

    // @Component로 등록된 사용자 정의 ArgumentResolver를 주입받아 사용한다.
    private final AuthUserArgumentResolver authUserArgumentResolver;
    private final AdminInterceptor adminInterceptor;

    // Spring MVC 초기화 과정에서 호출되어 사용자 정의 ArgumentResolver를 추가한다.
    // Bean 등록만으로는 MVC의 ArgumentResolver 목록에 자동 추가되지 않으므로 여기서 연결한다.
    @Override
    public void addArgumentResolvers(
            // Spring이 제공하는 사용자 정의 ArgumentResolver 등록용 목록이다.
            List<HandlerMethodArgumentResolver> resolvers
    ) {
        // 기본 Resolver들을 대체하지 않고, 사용자 정의 Resolver를 추가한다.
        // 이후 MVC가 Controller 인자를 준비할 때 supportsParameter()로 담당 여부를 확인하고,
        // 이 Resolver가 선택되면 resolveArgument()로 @Auth AuthUser에 전달할 값을 만든다.
        resolvers.add(authUserArgumentResolver);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminInterceptor).addPathPatterns("/admin/**");
    }

}
