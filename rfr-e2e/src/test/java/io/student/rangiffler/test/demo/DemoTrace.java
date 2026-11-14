package io.student.rangiffler.test.demo;

import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Связывает события расширения с его экземпляром и контекстом выполнения.
 */
interface DemoTrace {
    void record(DemoExtension extension, long creation, String event, ExtensionContext context);
}
