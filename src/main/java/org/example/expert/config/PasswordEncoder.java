package org.example.expert.config;

import at.favre.lib.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Component;

// Spring Bean으로 등록하여 AuthService 등에서 주입받아 사용한다.
@Component
public class PasswordEncoder {

    // 평문 비밀번호를 BCrypt로 해싱하여 저장용 문자열을 만든다.
    // 복호화 가능한 암호화와 달리, 해시에서 원래 비밀번호를 복원하는 방식은 제공하지 않는다.
    public String encode(String rawPassword) {
        // withDefaults(): 라이브러리의 기본 설정으로 BCrypt 해시 생성기를 얻는다.
        // MIN_COST: 이 라이브러리가 허용하는 최소 연산 비용이다. 비용이 커질수록 계산량이 늘어난다.
        // toCharArray(): 입력 문자열을 라이브러리에 전달할 char 배열로 변환한다.
        // 해싱할 때 무작위 salt를 생성하므로 같은 비밀번호도 호출마다 다른 해시가 나올 수 있다.
        // 반환 문자열에는 알고리즘 버전, 연산 비용, salt, 해시 정보가 포함된다.
        return BCrypt.withDefaults().hashToString(BCrypt.MIN_COST, rawPassword.toCharArray());
    }

    // 입력한 평문 비밀번호가 저장된 BCrypt 해시에 대응하는 비밀번호인지 검증한다.
    // 저장된 해시를 복호화하거나, encode()를 새로 호출한 결과와 문자열 비교를 하는 것이 아니다.
    public boolean matches(String rawPassword, String encodedPassword) {
        // 저장 문자열의 salt와 연산 비용 등을 사용하여 입력 비밀번호를 검증한다.
        BCrypt.Result result = BCrypt.verifyer().verify(rawPassword.toCharArray(), encodedPassword);
        // 검증 결과 중 일치 여부를 반환한다. 일치하면 true, 일치하지 않으면 false이다.
        return result.verified;
    }
}
