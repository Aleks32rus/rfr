package io.student.rangiffler.stand;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.IntPredicate;

/**
 * Проверяет готовность сервиса по статусу ответа HTTP.
 */
public final class HttpService implements Service {
    private final String name;
    private final String hint;
    private final HttpRequest request;
    private final IntPredicate accepted;

    public HttpService(String name, String hint, HttpRequest request, IntPredicate accepted) {
        this.name = name;
        this.hint = hint;
        this.request = request;
        this.accepted = accepted;
    }

    public HttpService(String name, String hint, URI uri) {
        this(name, hint, HttpRequest.newBuilder(uri).GET().build(), status -> status >= 200 && status < 300);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String hint() {
        return hint;
    }

    @Override
    public boolean responds() {
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5)).build();
        try {
            if ("https".equalsIgnoreCase(request.uri().getScheme())) {
                return responds(client, request.uri(), deadline);
            }
            for (InetAddress address : InetAddress.getAllByName(request.uri().getHost())) {
                URI uri = request.uri();
                String host = address.getHostAddress();
                String authority = (host.contains(":") ? "[" + host + "]" : host)
                        + (uri.getPort() < 0 ? "" : ":" + uri.getPort());
                URI target = URI.create(uri.getScheme() + "://" + authority + uri.getRawPath()
                        + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery()));
                if (responds(client, target, deadline)) {
                    return true;
                }
            }
            return false;
        } catch (UnknownHostException failure) {
            return false;
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Проверка сервиса прервана: " + name, failure);
        } finally {
            client.shutdownNow();
        }
    }

    private boolean responds(HttpClient client, URI target, long deadline) throws InterruptedException {
        long remaining = deadline - System.nanoTime();
        if (remaining <= 0) {
            return false;
        }
        HttpRequest probe = HttpRequest.newBuilder(request, (name, value) -> true).uri(target)
                .version(HttpClient.Version.HTTP_1_1).timeout(Duration.ofNanos(remaining)).build();
        CompletableFuture<HttpResponse<Void>> response = client.sendAsync(probe, HttpResponse.BodyHandlers.discarding());
        try {
            return accepted.test(response.get(Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS)
                    .statusCode());
        } catch (TimeoutException | ExecutionException failure) {
            return false;
        } finally {
            response.cancel(true);
        }
    }
}
