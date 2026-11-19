package io.student.rangiffler.junit.annotation;

import io.student.rangiffler.junit.extension.UsersQueueExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Запрашивает исключительное владение пользователем общего пула на время теста.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@ExtendWith(UsersQueueExtension.class)
public @interface StaticUser {
}
