package io.student.rangiffler.data;

import java.util.List;
import java.util.UUID;

/**
 * Данные созданного пользователя, его друзей и участников приглашений для теста.
 */
public record TestUser(UUID id, String username, String password, List<TestUser> incomeInvitations,
                       List<TestUser> friends, List<TestUser> outcomeInvitations) {

    public TestUser(UUID id, String username, String password) {
        this(id, username, password, List.of(), List.of(), List.of());
    }

    public TestUser(UUID id, String username, String password, List<TestUser> incomeInvitations) {
        this(id, username, password, incomeInvitations, List.of(), List.of());
    }

    public TestUser(UUID id, String username, String password, List<TestUser> incomeInvitations,
                    List<TestUser> friends) {
        this(id, username, password, incomeInvitations, friends, List.of());
    }

    public TestUser(UUID id, String username, String password, List<TestUser> incomeInvitations,
                    List<TestUser> friends, List<TestUser> outcomeInvitations) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.incomeInvitations = List.copyOf(incomeInvitations);
        this.friends = List.copyOf(friends);
        this.outcomeInvitations = List.copyOf(outcomeInvitations);
    }

    public TestUser withOutcomeInvitations(List<TestUser> invitations) {
        return new TestUser(id, username, password, incomeInvitations, friends, invitations);
    }

    @Override
    public String toString() {
        return "TestUser[id=" + id + ", username=" + username + ", password=***]";
    }
}
