package io.student.rangiffler.test;

import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.data.db.AuthAccount;
import io.student.rangiffler.data.db.Authorities;
import io.student.rangiffler.data.db.Profile;
import io.student.rangiffler.fixture.SingleUserFixture;
import io.student.rangiffler.fixture.UserFixture;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Проверяет создание и очистку пользователя в auth и API.
 */
@Tag("db")
final class SingleUserFixtureTest {
    @Test
    void creationCannotLeaveUserWithoutProfile() {
        try (UserFixture fixture = new SingleUserFixture(new TestUser(UUID.randomUUID(),
                "fixture_" + UUID.randomUUID(), "M7e4q9K2w8R5"))) {
            TestUser user = fixture.create();
            assertEquals(List.of(true, true, true), List.of(new AuthAccount(user).exist(),
                            new Authorities(user).exist(), new Profile(user).exist()),
                    "Пользователь не должен оставаться без аккаунта, прав или профиля API");
        }
    }

    @Test
    void closingCannotLeaveUserAndProfileOrFailWhenRepeated() {
        UserFixture fixture = new SingleUserFixture(new TestUser(UUID.randomUUID(),
                "fixture_" + UUID.randomUUID(), "M7e4q9K2w8R5"));
        TestUser user;
        try (fixture) {
            user = fixture.create();
            fixture.create();
        }
        fixture.close();
        assertEquals(List.of(false, false, false), List.of(new AuthAccount(user).exist(),
                        new Authorities(user).exist(), new Profile(user).exist()),
                "После закрытия аккаунт, права и профиль не должны оставаться в БД");
    }

    @Test
    void closingCannotLeaveAuthorities() {
        TestUser user;
        try (UserFixture fixture = new SingleUserFixture(new TestUser(UUID.randomUUID(),
                "rights_" + UUID.randomUUID(), "H8v3J6s9P2c7"))) {
            user = fixture.create();
        }
        assertFalse(new Authorities(user).exist(), "Права пользователя не должны оставаться после закрытия фикстуры");
    }
}
