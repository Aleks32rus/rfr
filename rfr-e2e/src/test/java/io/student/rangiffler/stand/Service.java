package io.student.rangiffler.stand;

/**
 * Описывает проверку готовности сервиса и способ его запуска.
 */
public interface Service {
    String name();

    String hint();

    boolean responds();
}
