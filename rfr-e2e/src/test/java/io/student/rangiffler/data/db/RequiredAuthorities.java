package io.student.rangiffler.data.db;

/**
 * Различает наличие строк прав и полноту обязательного набора аккаунта.
 */
public interface RequiredAuthorities extends StoredData {
    boolean complete();
}
