package io.student.rangiffler.data.db;

/**
 * Описывает создание, удаление и проверку наличия данных пользователя.
 */
public interface StoredData {
    void create();

    void delete();

    boolean exist();
}
