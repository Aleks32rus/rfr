package io.student.rangiffler.junit.extension;

import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.fixture.StaticUserFixture;
import io.student.rangiffler.fixture.UserFixture;
import io.student.rangiffler.junit.annotation.StaticUser;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * Передаёт пользователя общего пула и поручает Store возврат его фикстуры.
 */
public final class UsersQueueExtension implements ParameterResolver {
    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(UsersQueueExtension.class);

    @Override
    public boolean supportsParameter(ParameterContext parameter, ExtensionContext context) {
        return parameter.isAnnotated(StaticUser.class) && parameter.getParameter().getType().equals(TestUser.class);
    }

    @Override
    public TestUser resolveParameter(ParameterContext parameter, ExtensionContext context) {
        return context.getStore(NAMESPACE)
                .computeIfAbsent(parameter.getIndex(), index -> new StaticUserFixture(), UserFixture.class)
                .create();
    }
}
