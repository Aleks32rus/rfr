package io.student.rangiffler.stand;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Ждёт готовности стенда и подсказывает, как запустить недоступные сервисы.
 */
public final class Stand {
    private final List<Service> services;
    private final Duration timeout;
    private final Duration pause;

    public Stand(List<Service> services, Duration timeout, Duration pause) {
        this.services = List.copyOf(services);
        this.timeout = timeout;
        this.pause = pause;
    }

    public void await() {
        List<Service> pending = new ArrayList<>(services);
        long deadline = System.nanoTime() + timeout.toNanos();
        ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor();
        try {
            if (!pending.isEmpty() && deadline - System.nanoTime() > 0) {
                List<Service> round = List.copyOf(pending);
                List<Callable<Boolean>> probes = round.stream().<Callable<Boolean>>map(service -> service::responds)
                        .toList();
                List<Future<Boolean>> checks = pool.invokeAll(probes, Math.max(0, deadline - System.nanoTime()),
                        TimeUnit.NANOSECONDS);
                for (int index = 0; index < round.size(); index++) {
                    if (!checks.get(index).isCancelled() && checks.get(index).get()) {
                        pending.remove(round.get(index));
                    }
                }
                if (!pending.isEmpty()) {
                    TimeUnit.NANOSECONDS.sleep(Math.min(pause.toNanos(), Math.max(0, deadline - System.nanoTime())));
                }
            }
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Ожидание стенда прервано", failure);
        } catch (ExecutionException failure) {
            throw new IllegalStateException("Ошибка при проверке готовности стенда", failure.getCause());
        } finally {
            pool.shutdownNow();
        }
        if (!pending.isEmpty()) {
            throw unavailable(pending);
        }
    }

    private IllegalStateException unavailable(List<Service> pending) {
        return new IllegalStateException("Стенд не готов за " + timeout.toSeconds() + " с, недоступны "
                + pending.stream().map(service -> service.name() + " (" + service.hint() + ")")
                .collect(Collectors.joining(", ")));
    }
}
