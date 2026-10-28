package io.student.rangiffler.data;

import java.util.UUID;

/**
 * Данные созданного пользователя для авторизации в тесте.
 */
public record TestUser(UUID id, String username, String password) {

    @Override
    public String toString() {
        return "TestUser[id=" + id + ", username=" + username + ", password=***]";
    }
}
