package io.student.rangiffler.junit.extension;

import io.student.rangiffler.fixture.UserFixture;
import io.student.rangiffler.fixture.UserRequest;
import io.student.rangiffler.junit.annotation.User;
import io.student.rangiffler.data.TestUser;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * Передаёт подготовленного пользователя каждому параметру с аннотацией User.
 */
public final class UserExtension implements ParameterResolver {
    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(UserExtension.class);

    @Override
    public boolean supportsParameter(ParameterContext parameter, ExtensionContext context) {
        return parameter.isAnnotated(User.class) && parameter.getParameter().getType().equals(TestUser.class);
    }

    @Override
    public TestUser resolveParameter(ParameterContext parameter, ExtensionContext context) {
        return context.getStore(NAMESPACE)
                .computeIfAbsent(parameter.getIndex(), index -> new UserRequest(
                        parameter.findAnnotation(User.class).orElseThrow()).fixture(), UserFixture.class)
                .create();
    }
}
