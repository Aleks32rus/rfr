package io.student.rangiffler.test.demo;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Отмечает строковый параметр, который предоставляет DemoExtension.
 */
@Target(PARAMETER)
@Retention(RUNTIME)
@interface DemoParameter {
}
