package io.student.rangiffler.test.fake;

import io.qameta.allure.Allure;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Проверка выполнения шага Allure")
public class SimpleTest {

    @Test
    @DisplayName("Шаг Allure выполняет проверку суммы")
    void simpleTest() {
        Allure.step("Simple test", () -> {
            assertEquals(2, 1 + 1);
        });
    }
}
