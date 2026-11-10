package io.student.rangiffler.junit.annotation;

import io.student.rangiffler.junit.extension.ProbeTargetCondition;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Ограничивает выполнение мишени явно разрешённым запуском EngineTestKit.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@ExtendWith(ProbeTargetCondition.class)
public @interface ProbeTarget {
}
