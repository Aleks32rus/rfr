package io.student.rangiffler.test;

import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.fixture.StaticUserFixture;
import io.student.rangiffler.fixture.UserFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Проверяет ограниченную выдачу, возврат и исключительное владение пользователями пула.
 */
@Timeout(value = 8, unit = TimeUnit.SECONDS)
@DisplayName("Получение и возврат пользователя общего пула")
final class StaticUserFixtureTest {
    @Test
    @DisplayName("Повторное создание не забирает второго пользователя из пула")
    void repeatedCreationCannotBorrowAnotherUser() {
        try (UserFixture first = new StaticUserFixture(Duration.ofSeconds(2));
             UserFixture second = new StaticUserFixture(Duration.ofSeconds(2));
             UserFixture third = new StaticUserFixture(Duration.ofSeconds(2))) {
            TestUser user = first.create();
            TestUser repeated = first.create();
            assertThat(List.of(user.equals(repeated),
                    List.of(first.user(), second.create(), third.create()).stream().distinct().count()))
                    .as("Повторное создание не должно менять пользователя или уменьшать доступную ёмкость пула")
                    .isEqualTo(List.of(true, 3L));
        }
    }

    @Test
    @DisplayName("Пустая очередь завершается ошибкой после ограниченного ожидания")
    void exhaustedPoolCannotWaitForever() {
        try (UserFixture first = new StaticUserFixture(Duration.ofSeconds(2));
             UserFixture second = new StaticUserFixture(Duration.ofSeconds(2));
             UserFixture third = new StaticUserFixture(Duration.ofSeconds(2));
             UserFixture waiting = new StaticUserFixture(Duration.ofSeconds(1))) {
            first.create();
            second.create();
            third.create();
            assertThatThrownBy(waiting::create)
                    .as("Пустая очередь не должна ожидать пользователя бесконечно или выдавать неясную ошибку")
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Не удалось получить пользователя из пула за PT1S: все три пользователя заняты");
        }
    }

    @Test
    @DisplayName("Закрытие возвращает только взятого пользователя и не дублирует его")
    void closingCannotReturnUnborrowedOrDuplicateUser() {
        try (UserFixture returned = new StaticUserFixture(Duration.ofSeconds(2));
             UserFixture unused = new StaticUserFixture(Duration.ofSeconds(2))) {
            returned.create();
            returned.close();
            returned.close();
            unused.close();
            try (UserFixture first = new StaticUserFixture(Duration.ofSeconds(2));
                 UserFixture second = new StaticUserFixture(Duration.ofSeconds(2));
                 UserFixture third = new StaticUserFixture(Duration.ofSeconds(2));
                 UserFixture waiting = new StaticUserFixture(Duration.ofSeconds(1))) {
                first.create();
                second.create();
                third.create();
                assertThatThrownBy(waiting::create)
                        .as("Закрытие не должно добавлять в очередь лишних или повторных пользователей")
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("все три пользователя заняты");
            }
        }
    }

    @Test
    @DisplayName("Четыре потока не получают одного пользователя одновременно")
    void concurrentFixturesCannotShareBorrowedUser() throws Exception {
        Set<UUID> held = ConcurrentHashMap.newKeySet();
        List<UUID> collisions = new CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(3);
        try (ExecutorService executor = Executors.newFixedThreadPool(4)) {
            List<Future<UUID>> tasks = new ArrayList<>();
            for (int index = 0; index < 4; index++) {
                tasks.add(executor.submit(() -> {
                    try (UserFixture fixture = new StaticUserFixture(Duration.ofSeconds(2))) {
                        TestUser user = fixture.create();
                        if (!held.add(user.id())) {
                            collisions.add(user.id());
                        }
                        latch.countDown();
                        try {
                            if (!latch.await(3, TimeUnit.SECONDS)) {
                                throw new IllegalStateException("Не удалось одновременно получить трёх пользователей пула");
                            }
                            return user.id();
                        } finally {
                            held.remove(user.id());
                        }
                    }
                }));
            }
            List<UUID> results = new ArrayList<>();
            for (Future<UUID> task : tasks) {
                results.add(task.get(5, TimeUnit.SECONDS));
            }
            assertThat(List.of(results.size(), results.stream().distinct().count(), collisions.size(), held.isEmpty()))
                    .as("Конкурентная выдача не должна дублировать владение или оставлять незавершённые аренды")
                    .isEqualTo(List.of(4, 3L, 0, true));
        }
    }
}
