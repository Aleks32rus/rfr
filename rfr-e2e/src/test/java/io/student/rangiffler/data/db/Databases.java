package io.student.rangiffler.data.db;

import io.student.rangiffler.config.TestConfig;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.JdbcTransactionManager;

/**
 * Источники данных и соответствующие менеджеры транзакций баз auth и API.
 */
public enum Databases {
    AUTH("rangiffler-auth", TestConfig.AUTH_DB_URL, TestConfig.AUTH_DB_USERNAME, TestConfig.AUTH_DB_PASSWORD),
    API("rangiffler-api", TestConfig.API_DB_URL, TestConfig.API_DB_USERNAME, TestConfig.API_DB_PASSWORD);

    private final String schema;
    private final DataSource dataSource;
    private final JdbcTransactionManager transactionManager;

    Databases(String schema, String url, String username, String password) {
        this.schema = schema;
        this.dataSource = new DriverManagerDataSource(url, username, password);
        this.transactionManager = new JdbcTransactionManager(dataSource);
    }

    public DataSource dataSource() {
        return dataSource;
    }

    public String schema() {
        return schema;
    }

    public JdbcTransactionManager transactionManager() {
        return transactionManager;
    }
}
