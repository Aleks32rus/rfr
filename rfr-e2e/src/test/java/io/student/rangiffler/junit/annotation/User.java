package io.student.rangiffler.junit.annotation;

import io.student.rangiffler.junit.extension.UserExtension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Описывает пользователя и его связи для аннотированного параметра TestUser.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@ExtendWith(UserExtension.class)
public @interface User {
    int friends() default 0;

    int incomeInvitations() default 0;
}
