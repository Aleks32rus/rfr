package io.student.rangiffler.test.demo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Демонстрирует независимую регистрацию расширения на всех целях ExtendWith.
 * Цель PARAMETER также допускает параметры конструктора и методов жизненного цикла.
 * Размещение определяет область регистрации, поэтому набор вызванных callbacks различается.
 * DemoLifecycle выводит дерево: одинаковый E-id означает один и тот же экземпляр расширения.
 * Узел constructor фиксирует создание; время конструктора сохраняется до первого callback.
 * Учебное исключение согласовано: регистрации вне класса и статического поля намеренно
 * нарушают правило проекта для расширений с BeforeAllCallback и AfterAllCallback.
 */
@DisplayName("Места регистрации ExtendWith")
@ExtendWith(DemoLifecycle.class)
final class ExtendWithDemoTest {
    /**
     * Подключает расширение для всех тестов класса.
     */
    @Nested
    @DisplayName("Аннотация на классе")
    @ExtendWith(DemoExtension.class)
    final class ClassPlacement {
        @Test
        @DisplayName("Первый тест: Расширение класса разрешает параметр")
        void classRegistrationCannotLeaveParameterUnresolved(@DemoParameter String value, DemoBody body) {
            body.log();
            assertEquals("Демонстрационный параметр", value, "Расширение класса не должно оставлять параметр неразрешённым");
        }

        @Test
        @DisplayName("Второй тест: Расширение класса разрешает параметр")
        void secondClassRegistrationCannotLeaveParameterUnresolved(@DemoParameter String value, DemoBody body) {
            body.log();
            assertEquals("Демонстрационный параметр", value, "Расширение класса не должно оставлять параметр неразрешённым");
        }
    }

    /**
     * Ограничивает регистрацию отдельным тестовым методом.
     */
    @Nested
    @DisplayName("Аннотация на методе")
    final class MethodPlacement {
        @Test
        @DisplayName("Первый тест: Расширение метода разрешает параметр")
        @ExtendWith(DemoExtension.class)
        void methodRegistrationCannotLeaveParameterUnresolved(@DemoParameter String value, DemoBody body) {
            body.log();
            assertEquals("Демонстрационный параметр", value, "Расширение метода не должно оставлять параметр неразрешённым");
        }

        @Test
        @DisplayName("Второй тест: Расширение метода разрешает параметр")
        @ExtendWith(DemoExtension.class)
        void secondMethodRegistrationCannotLeaveParameterUnresolved(@DemoParameter String value, DemoBody body) {
            body.log();
            assertEquals("Демонстрационный параметр", value, "Расширение метода не должно оставлять параметр неразрешённым");
        }
    }

    /**
     * Показывает регистрацию через поле без внедрения значения в поле.
     */
    @Nested
    @DisplayName("Аннотация на поле экземпляра")
    final class FieldPlacement {
        @ExtendWith(DemoExtension.class)
        private final String registration = "Поле служит местом регистрации, его значение JUnit не изменяет";

        @Test
        @DisplayName("Первый тест: Расширение поля разрешает параметр")
        void fieldRegistrationCannotLeaveParameterUnresolved(@DemoParameter String value, DemoBody body) {
            body.log();
            assertEquals("Демонстрационный параметр", value, "Расширение поля не должно оставлять параметр неразрешённым");
        }

        @Test
        @DisplayName("Второй тест: Расширение поля разрешает параметр")
        void secondFieldRegistrationCannotLeaveParameterUnresolved(@DemoParameter String value, DemoBody body) {
            body.log();
            assertEquals("Демонстрационный параметр", value, "Расширение поля не должно оставлять параметр неразрешённым");
        }
    }

    /**
     * Регистрирует расширение до создания экземпляра через статическое поле.
     */
    @Nested
    @DisplayName("Аннотация на статическом поле")
    final class StaticFieldPlacement {
        @ExtendWith(DemoExtension.class)
        private static final String registration = "Статическое поле регистрирует расширение на уровне класса";

        @Test
        @DisplayName("Первый тест: Расширение статического поля разрешает параметр")
        void staticFieldRegistrationCannotLeaveParameterUnresolved(@DemoParameter String value, DemoBody body) {
            body.log();
            assertEquals("Демонстрационный параметр", value, "Расширение статического поля не должно оставлять параметр неразрешённым");
        }

        @Test
        @DisplayName("Второй тест: Расширение статического поля разрешает параметр")
        void secondStaticFieldRegistrationCannotLeaveParameterUnresolved(@DemoParameter String value, DemoBody body) {
            body.log();
            assertEquals("Демонстрационный параметр", value, "Расширение статического поля не должно оставлять параметр неразрешённым");
        }
    }

    /**
     * Регистрирует расширение непосредственно на параметре тестового метода.
     */
    @Nested
    @DisplayName("Аннотация на параметре")
    final class ParameterPlacement {
        @Test
        @DisplayName("Первый тест: Расширение параметра разрешает значение")
        void parameterRegistrationCannotLeaveParameterUnresolved(
                @ExtendWith(DemoExtension.class) @DemoParameter String value, DemoBody body) {
            body.log();
            assertEquals("Демонстрационный параметр", value, "Расширение параметра не должно оставлять значение неразрешённым");
        }

        @Test
        @DisplayName("Второй тест: Расширение параметра разрешает значение")
        void secondParameterRegistrationCannotLeaveParameterUnresolved(
                @ExtendWith(DemoExtension.class) @DemoParameter String value, DemoBody body) {
            body.log();
            assertEquals("Демонстрационный параметр", value, "Расширение параметра не должно оставлять значение неразрешённым");
        }
    }

    /**
     * Переносит регистрацию расширения в составную аннотацию.
     */
    @Target(TYPE)
    @Retention(RUNTIME)
    @ExtendWith(DemoExtension.class)
    @interface Demo {
    }

    /**
     * Подключает расширение через собственную аннотацию.
     */
    @Nested
    @DisplayName("Аннотация на собственной аннотации")
    @Demo
    final class AnnotationPlacement {
        @Test
        @DisplayName("Первый тест: Составная аннотация подключает расширение")
        void composedRegistrationCannotLeaveParameterUnresolved(@DemoParameter String value, DemoBody body) {
            body.log();
            assertEquals("Демонстрационный параметр", value, "Составная аннотация не должна оставлять параметр неразрешённым");
        }

        @Test
        @DisplayName("Второй тест: Составная аннотация подключает расширение")
        void secondComposedRegistrationCannotLeaveParameterUnresolved(@DemoParameter String value, DemoBody body) {
            body.log();
            assertEquals("Демонстрационный параметр", value, "Составная аннотация не должна оставлять параметр неразрешённым");
        }
    }

    /**
     * Передаёт регистрацию расширения реализующему тестовому классу.
     */
    @ExtendWith(DemoExtension.class)
    interface DemoContract {
    }

    /**
     * Получает расширение от тестового интерфейса.
     */
    @Nested
    @DisplayName("Аннотация на интерфейсе")
    final class InterfacePlacement implements DemoContract {
        @Test
        @DisplayName("Первый тест: Интерфейс подключает расширение")
        void interfaceRegistrationCannotLeaveParameterUnresolved(@DemoParameter String value, DemoBody body) {
            body.log();
            assertEquals("Демонстрационный параметр", value, "Аннотация интерфейса не должна оставлять параметр неразрешённым");
        }

        @Test
        @DisplayName("Второй тест: Интерфейс подключает расширение")
        void secondInterfaceRegistrationCannotLeaveParameterUnresolved(@DemoParameter String value, DemoBody body) {
            body.log();
            assertEquals("Демонстрационный параметр", value, "Аннотация интерфейса не должна оставлять параметр неразрешённым");
        }
    }
}
