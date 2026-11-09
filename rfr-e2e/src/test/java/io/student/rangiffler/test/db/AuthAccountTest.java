package io.student.rangiffler.test.db;

import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.data.db.AuthAccount;
import io.student.rangiffler.data.db.Authorities;
import io.student.rangiffler.fixture.SingleUserFixture;
import io.student.rangiffler.fixture.UserFixture;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Проверяет согласованное создание и удаление аккаунта и прав.
 */
@Tag("db")
final class AuthAccountTest {
    @Test
    void creationCannotOmitAuthorities() {
        TestUser user = new TestUser(UUID.randomUUID(), "account_" + UUID.randomUUID(), "K8v3R6p2F9s4");
        try (UserFixture fixture = new SingleUserFixture(user)) {
            new AuthAccount(user).create();
            assertEquals(List.of(true, true), List.of(new AuthAccount(user).exist(), new Authorities(user).exist()),
                    "Аккаунт не должен создаваться без обязательных прав");
        }
    }

    @Test
    void deletionCannotLeaveAccountOrAuthorities() {
        TestUser user = new TestUser(UUID.randomUUID(), "account_" + UUID.randomUUID(), "P7m2Z9t4C6r3");
        try (UserFixture fixture = new SingleUserFixture(user)) {
            AuthAccount account = new AuthAccount(user);
            account.create();
            account.delete();
            assertEquals(List.of(false, false), List.of(account.exist(), new Authorities(user).exist()),
                    "Аккаунт и его права не должны оставаться после удаления");
        }
    }
}
