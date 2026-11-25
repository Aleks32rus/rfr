package io.student.rangiffler.data.db;

/**
 * Проверяет соответствие схемы миграциям текущей ветки без изменения базы данных.
 */
public interface SchemaCompatibility {
    void verify();
}
