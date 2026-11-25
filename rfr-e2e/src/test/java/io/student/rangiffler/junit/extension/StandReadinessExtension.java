package io.student.rangiffler.junit.extension;

import io.student.rangiffler.config.TestConfig;
import io.student.rangiffler.stand.HttpService;
import io.student.rangiffler.stand.Stand;
import io.student.rangiffler.stand.TcpService;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.List;

/**
 * Ждёт готовности стенда перед запуском UI-классов.
 */
public final class StandReadinessExtension implements SuiteExtension {
    private final boolean enabled;
    private final String tag;
    private final Stand stand;

    public StandReadinessExtension(boolean enabled, String tag, Stand stand) {
        this.enabled = enabled;
        this.tag = tag;
        this.stand = stand;
    }

    public StandReadinessExtension() {
        this(TestConfig.standCheck(), "ui", new Stand(List.of(
                new TcpService("MySQL", "docker compose up -d mysql --wait", TestConfig.dbAddress()),
                new HttpService("rfr-auth", "./gradlew runAll",
                        URI.create(TestConfig.AUTH_URL).resolve("/.well-known/openid-configuration")),
                new HttpService("rfr-api", "./gradlew runAll",
                        HttpRequest.newBuilder(URI.create(TestConfig.API_URL).resolve("/graphql"))
                                .header("Content-Type", "application/json")
                                .POST(HttpRequest.BodyPublishers.ofString("{\"query\":\"{__typename}\"}")).build(),
                        status -> true),
                new HttpService("rfr-gql-client", "npm run dev", URI.create(TestConfig.FRONT_URL))
        ), TestConfig.standTimeout(), Duration.ofSeconds(1)));
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        if (context.getTags().contains(tag)) {
            SuiteExtension.super.beforeAll(context);
        }
    }

    @Override
    public void beforeSuite(ExtensionContext context) {
        if (enabled) {
            stand.await();
        }
    }
}
