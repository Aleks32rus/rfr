package io.student.rangiffler.junit.extension;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Подключает подготовку и закрытие расширения один раз на весь запуск движка Jupiter.
 */
public interface SuiteExtension extends BeforeAllCallback {
    @Override
    default void beforeAll(ExtensionContext context) throws Exception {
        ExtensionContext root = context.getRoot();
        root.getStore(ExtensionContext.Namespace.create(SuiteExtension.class))
                .computeIfAbsent(getClass(), key -> new SuiteRun(this, root), SuiteRun.class).start();
    }

    default void beforeSuite(ExtensionContext context) {
    }

    default void afterSuite(ExtensionContext context) {
    }
}
