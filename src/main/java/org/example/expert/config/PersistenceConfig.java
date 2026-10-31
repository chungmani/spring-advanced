package org.example.expert.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

// Persistence는 데이터를 DB 등에 저장하여 유지하는 '영속성'을 뜻한다.
// 이 클래스를 Spring 설정 클래스로 등록한다. 요청이 순서대로 거쳐 가는 필터 같은 객체는 아니다.
@Configuration
// Spring Data JPA의 Auditing 기능을 활성화한다.
// Auditing은 엔티티의 생성·수정 시각이나 생성·수정 주체 등을 자동으로 기록하는 기능이다.
// 현재 프로젝트에서는 Timestamped의 @CreatedDate와 @LastModifiedDate로 시각을 기록한다.
// Timestamped의 AuditingEntityListener가 JPA의 저장·수정 이벤트에 맞춰 날짜 값을 채운다.
// 생성 시에는 기본적으로 생성 시각과 수정 시각이 함께 설정되고, 수정 시에는 수정 시각이 갱신된다.
// 이 설정만으로 모든 엔티티에 날짜 필드가 생기지는 않는다. 엔티티 쪽의 필드·리스너 설정도 필요하다.
@EnableJpaAuditing
public class PersistenceConfig {
    // 직접 호출할 메서드 없이, 위 어노테이션으로 Auditing 기능을 켜는 역할을 한다.
}
