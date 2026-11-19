package io.student.rangiffler.test.db;

import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.data.db.FriendshipStatus;
import io.student.rangiffler.data.db.Friendships;
import io.student.rangiffler.data.db.Profile;
import io.student.rangiffler.fixture.ReservedUserFixture;
import io.student.rangiffler.fixture.SingleUserFixture;
import io.student.rangiffler.fixture.UserFixture;
import io.student.rangiffler.fixture.UserReservation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Проверяет профиль, обязательную страну и очистку связей владельца.
 */
@Tag("db")
@DisplayName("Профиль пользователя в БД")
final class ProfileTest {
    @Test
    @DisplayName("Созданный профиль присутствует в БД")
    void createdProfileCannotBeAbsentFromDatabase() {
        TestUser user = new TestUser(UUID.randomUUID(), "profile_" + UUID.randomUUID(), "M9c4V7s2Q8n3");
        try (UserReservation fixture = new ReservedUserFixture(user)) {
            new Profile(user, "fr").create();
            assertTrue(new Profile(user, "ca").exist(), "Созданный профиль не должен отсутствовать в БД");
        }
    }

    @Test
    @DisplayName("Создание профиля отклоняет неизвестную страну")
    void creationCannotUseUnknownCountry() {
        TestUser user = new TestUser(UUID.randomUUID(), "profile_" + UUID.randomUUID(), "H2f9W4k7J3v8");
        try (UserReservation fixture = new ReservedUserFixture(user)) {
            assertThrows(DataIntegrityViolationException.class, () -> new Profile(user, "missing-country-47").create(),
                    "Профиль не должен создаваться с несуществующей страной");
        }
    }

    @Test
    @DisplayName("Удаление профиля удаляет дружбу и сохраняет профиль второго участника")
    void deletionCannotLeaveFriendshipOrDeletePeerProfile() {
        try (UserFixture owner = new SingleUserFixture(new TestUser(UUID.randomUUID(),
                "owner_" + UUID.randomUUID(), "D3r8B2p6T9k4"));
             UserFixture peer = new SingleUserFixture(new TestUser(UUID.randomUUID(),
                     "peer_" + UUID.randomUUID(), "A7w2S9n4G6m3"))) {
            new Friendships(owner.create()).create(peer.create(), FriendshipStatus.ACCEPTED);
            new Profile(owner.user(), "fr").delete();
            assertEquals(List.of(false, false, true), List.of(new Profile(owner.user()).exist(),
                            new Friendships(peer.user()).exist(owner.user(), FriendshipStatus.ACCEPTED),
                            new Profile(peer.user()).exist()),
                    "Удаление профиля не должно оставлять дружбу или удалять профиль второго участника");
        }
    }
}
