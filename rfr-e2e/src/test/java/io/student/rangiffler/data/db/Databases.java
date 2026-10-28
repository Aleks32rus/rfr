package io.student.rangiffler.data.db;

import io.student.rangiffler.config.TestConfig;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.JdbcTransactionManager;

/**
 * Источники данных и соответствующие менеджеры транзакций баз auth и API.
 */
public enum Databases {
    AUTH(TestConfig.AUTH_DB_URL, TestConfig.AUTH_DB_USERNAME, TestConfig.AUTH_DB_PASSWORD),
    API(TestConfig.API_DB_URL, TestConfig.API_DB_USERNAME, TestConfig.API_DB_PASSWORD);

    private final DataSource dataSource;
    private final JdbcTransactionManager transactionManager;

    Databases(String url, String username, String password) {
        this.dataSource = new DriverManagerDataSource(url, username, password);
        this.transactionManager = new JdbcTransactionManager(dataSource);
    }

    public DataSource dataSource() {
        return dataSource;
    }

    public JdbcTransactionManager transactionManager() {
        return transactionManager;
    }
}
