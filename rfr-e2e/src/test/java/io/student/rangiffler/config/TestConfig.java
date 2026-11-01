package io.student.rangiffler.config;

import java.util.Locale;

/**
 * Адреса окружения: системные свойства имеют приоритет над переменными окружения и значениями по умолчанию.
 */
public final class TestConfig {
    private static final String DEFAULT_FRONT_URL = "http://localhost:3001";
    private static final String DEFAULT_AUTH_URL = "http://localhost:9001";
    private static final String DEFAULT_API_URL = "http://localhost:8081";
    private static final String DEFAULT_AUTH_DB_URL = "jdbc:mysql://localhost:3306/rangiffler-auth?serverTimezone=UTC";
    private static final String DEFAULT_AUTH_DB_USERNAME = "root";
    private static final String DEFAULT_AUTH_DB_PASSWORD = "secret";
    private static final String DEFAULT_API_DB_URL = "jdbc:mysql://localhost:3306/rangiffler-api?serverTimezone=UTC";
    private static final String DEFAULT_API_DB_USERNAME = "root";
    private static final String DEFAULT_API_DB_PASSWORD = "secret";

    public static final String FRONT_URL = value("front.url", DEFAULT_FRONT_URL);
    public static final String AUTH_URL = value("auth.url", DEFAULT_AUTH_URL);
    public static final String API_URL = value("api.url", DEFAULT_API_URL);
    public static final String AUTH_DB_URL = value("auth.db.url", DEFAULT_AUTH_DB_URL);
    public static final String AUTH_DB_USERNAME = value("auth.db.username", DEFAULT_AUTH_DB_USERNAME);
    public static final String AUTH_DB_PASSWORD = value("auth.db.password", DEFAULT_AUTH_DB_PASSWORD);
    public static final String API_DB_URL = value("api.db.url", DEFAULT_API_DB_URL);
    public static final String API_DB_USERNAME = value("api.db.username", DEFAULT_API_DB_USERNAME);
    public static final String API_DB_PASSWORD = value("api.db.password", DEFAULT_API_DB_PASSWORD);

    private TestConfig() {
    }

    private static String value(String key, String fallback) {
        return System.getProperty("rfr." + key,
                System.getenv().getOrDefault("RFR_" + key.replace('.', '_').toUpperCase(Locale.ROOT), fallback));
    }
}
