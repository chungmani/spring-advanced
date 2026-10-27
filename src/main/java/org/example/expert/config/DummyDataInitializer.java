package org.example.expert.config;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.expert.domain.todo.entity.Todo;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/** N+1 복습용 데이터. dummy 프로필을 명시했을 때만 실행합니다. */
@Slf4j
@Component
@Profile("dummy")
@RequiredArgsConstructor
public class DummyDataInitializer implements ApplicationRunner {

    private static final int USER_COUNT = 20;
    private static final int TODOS_PER_USER = 3;
    private static final String TITLE_PREFIX = "[N+1 dummy] ";
    private static final String PASSWORD = "1234";

    private final EntityManager entityManager;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate transactionTemplate;
    private final ConfigurableApplicationContext applicationContext;

    @Value("${dummy.exit-after-seed:false}")
    private boolean exitAfterSeed;

    @Override
    public void run(ApplicationArguments args) {
        transactionTemplate.executeWithoutResult(status -> initialize());
        log.info("더미 데이터 확인 완료: User 20명, Todo 60개, Manager 60개 (트랜잭션 커밋 완료)");

        // CLI에서 일회성 입력 시 사용. 일반 dummy 프로필 실행은 앱을 유지합니다.
        if (exitAfterSeed) {
            applicationContext.close();
        }
    }

    private void initialize() {
        List<String> emails = IntStream.rangeClosed(1, USER_COUNT)
                .mapToObj(index -> "test%d@test.com".formatted(index))
                .toList();

        migrateLegacyAccounts(emails);
        long existingUsers = countUsers(emails);
        if (existingUsers == 0) {
            List<User> users = new ArrayList<>();
            String encodedPassword = passwordEncoder.encode(PASSWORD);
            for (String email : emails) {
                User user = new User(email, encodedPassword, UserRole.USER);
                entityManager.persist(user);
                users.add(user);
            }

            // 최근 목록에도 서로 다른 작성자가 섞이도록 라운드별로 생성합니다.
            String[] weathers = {"Sunny", "Cloudy", "Rainy"};
            for (int round = 1; round <= TODOS_PER_USER; round++) {
                for (int index = 0; index < users.size(); index++) {
                    Todo todo = new Todo(
                            TITLE_PREFIX + "user%02d-todo%02d".formatted(index + 1, round),
                            "N+1 조회 비교용 더미 일정입니다.",
                            weathers[(index + round - 1) % weathers.length],
                            users.get(index)
                    );
                    entityManager.persist(todo);
                }
            }
            entityManager.flush();
            log.info("N+1 복습용 더미 데이터를 생성했습니다.");
        } else {
            log.info("기존 더미 계정을 발견했습니다. 중복 생성 없이 구성을 확인합니다.");
        }

        long users = countUsers(emails);
        long todos = entityManager.createQuery(
                        "select count(t) from Todo t where t.user.email in :emails and t.title like :title", Long.class)
                .setParameter("emails", emails)
                .setParameter("title", TITLE_PREFIX + "%")
                .getSingleResult();
        long managers = entityManager.createQuery(
                        "select count(m) from Manager m where m.todo.user.email in :emails and m.todo.title like :title", Long.class)
                .setParameter("emails", emails)
                .setParameter("title", TITLE_PREFIX + "%")
                .getSingleResult();

        log.info("더미 데이터 집계: User={}, Todo={}, Manager={}", users, todos, managers);
        if (users != USER_COUNT || todos != USER_COUNT * TODOS_PER_USER || managers != todos) {
            throw new IllegalStateException("더미 데이터 구성이 예상과 다릅니다. 기존 데이터를 확인해주세요. 자동 삭제하거나 덮어쓰지 않습니다.");
        }

        List<String> latestAuthors = entityManager.createQuery(
                        "select t.user.email from Todo t where t.user.email in :emails and t.title like :title "
                                + "order by t.modifiedAt desc, t.id desc", String.class)
                .setParameter("emails", emails)
                .setParameter("title", TITLE_PREFIX + "%")
                .setMaxResults(10)
                .getResultList();
        long distinctAuthors = latestAuthors.stream().distinct().count();
        log.info("최근 더미 Todo 10개 작성자 수: {}", distinctAuthors);
        if (distinctAuthors != 10) {
            throw new IllegalStateException("최근 더미 Todo의 작성자 분포를 확인해주세요.");
        }

        List<User> dummyUsers = entityManager.createQuery(
                        "select u from User u where u.email in :emails", User.class)
                .setParameter("emails", emails)
                .getResultList();
        if (dummyUsers.stream().anyMatch(user -> !passwordEncoder.matches(PASSWORD, user.getPassword()))) {
            throw new IllegalStateException("더미 계정의 비밀번호가 예상과 다릅니다.");
        }
        log.info("더미 계정 20개의 비밀번호 일치 검증 완료");
    }

    private void migrateLegacyAccounts(List<String> emails) {
        List<String> legacyEmails = IntStream.rangeClosed(1, USER_COUNT)
                .mapToObj(index -> "nplus1-user%02d@example.com".formatted(index))
                .toList();
        long legacyCount = countUsers(legacyEmails);
        if (legacyCount == 0) {
            return;
        }
        if (legacyCount != USER_COUNT || countUsers(emails) != 0) {
            throw new IllegalStateException("기존 더미 계정이 일부 없거나 새 이메일과 충돌합니다. 계정을 덮어쓰지 않습니다.");
        }

        String encodedPassword = passwordEncoder.encode(PASSWORD);
        for (int index = 0; index < USER_COUNT; index++) {
            entityManager.createQuery(
                            "update User u set u.email = :email, u.password = :password where u.email = :legacyEmail")
                    .setParameter("email", emails.get(index))
                    .setParameter("password", encodedPassword)
                    .setParameter("legacyEmail", legacyEmails.get(index))
                    .executeUpdate();
        }
        log.info("기존 더미 계정의 이메일과 비밀번호 변경 완료 (ID와 연관관계 유지)");
    }

    private long countUsers(List<String> emails) {
        return entityManager.createQuery("select count(u) from User u where u.email in :emails", Long.class)
                .setParameter("emails", emails)
                .getSingleResult();
    }
}
