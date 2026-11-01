package io.student.rangiffler.utils;

import java.util.UUID;

/**
 * Генерирует уникальные данные, соответствующие ограничениям формы регистрации.
 */
public final class RandomDataUtils {

    private RandomDataUtils() {
    }

    public static String randomUsername() {
        return "rfr_" + UUID.randomUUID().toString().replace("-", "");
    }

    public static String randomPassword() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
