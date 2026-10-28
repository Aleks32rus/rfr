package io.student.rangiffler.data.db;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Доступ к JDBC-клиентам, транзакциям и SQL из ресурсов тестового проекта.
 */
public final class Jdbc {

    private Jdbc() {
    }

    public static JdbcClient client(Databases database) {
        return JdbcClient.create(database.dataSource());
    }

    public static <T> T inTransaction(Databases database, Function<JdbcClient, T> action) {
        return new TransactionTemplate(database.transactionManager()).execute(status -> action.apply(client(database)));
    }

    public static String sql(String resource) {
        try {
            return new ClassPathResource(resource).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException("Не удалось загрузить SQL из ресурса: " + resource, exception);
        }
    }
}
