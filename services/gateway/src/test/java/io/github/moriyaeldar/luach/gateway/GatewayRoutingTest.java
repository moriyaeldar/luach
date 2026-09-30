package io.github.moriyaeldar.luach.gateway;

import com.sun.net.httpserver.HttpServer;
import io.github.moriyaeldar.luach.gateway.auth.SessionTokens;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseCookie;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

/** Starts two tiny fake services and checks routing, login and token forwarding end to end. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "luach.cookie.secure=false")
class GatewayRoutingTest {

    /** Last headers each fake service received, by service name. */
    private static final Map<String, Map<String, String>> received = new ConcurrentHashMap<>();
    private static final HttpServer taskService = fakeService("task-service");
    private static final HttpServer householdService = fakeService("household-service");

    @LocalServerPort
    private int port;

    @Autowired
    private SessionTokens tokens;

    @DynamicPropertySource
    static void routes(DynamicPropertyRegistry registry) {
        registry.add("TASK_SERVICE_URL", () -> "http://localhost:" + taskService.getAddress().getPort());
        registry.add("HOUSEHOLD_SERVICE_URL", () -> "http://localhost:" + householdService.getAddress().getPort());
    }

    @AfterAll
    static void stop() {
        taskService.stop(0);
        householdService.stop(0);
    }

    @BeforeEach
    void clear() {
        received.clear();
    }

    private WebTestClient client() {
        return WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    private String demoLogin() {
        ResponseCookie cookie = client().post().uri("/auth/demo").header("X-Requested-With", "luach").exchange()
                .expectStatus().isNoContent()
                .returnResult(Void.class).getResponseCookies().getFirst(SessionTokens.COOKIE);
        assertThat(cookie).isNotNull();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("Lax");
        return cookie.getValue();
    }

    @Test
    void apiNeedsALogin() {
        client().get().uri("/api/week?start=2026-10-04").exchange().expectStatus().isUnauthorized();
        assertThat(received).isEmpty();
    }

    @Test
    @DisplayName("Demo login sets a session cookie; the services get it as a bearer token, never as a cookie")
    void demoLoginAndTokenRelay() {
        String token = demoLogin();

        client().get().uri("/api/week?start=2026-10-04").cookie(SessionTokens.COOKIE, token).exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("task-service:/api/week");

        Map<String, String> headers = received.get("task-service");
        assertThat(headers.get("authorization")).isEqualTo("Bearer " + token);
        assertThat(headers).doesNotContainKey("cookie");

        var jwt = tokens.decoder().decode(token).block();
        assertThat(jwt.getSubject()).startsWith("demo:");
        assertThat(jwt.getClaimAsBoolean("demo")).isTrue();
    }

    @Test
    void routesHouseholdCallsToTheHouseholdService() {
        String token = demoLogin();
        client().get().uri("/api/me").cookie(SessionTokens.COOKIE, token).exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("household-service:/api/me");
    }

    @Test
    @DisplayName("An invite can be previewed before logging in")
    void invitePreviewIsPublic() {
        client().get().uri("/api/invites/abc").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("household-service:/api/invites/abc");
        assertThat(received.get("household-service")).doesNotContainKey("authorization");
    }

    @Test
    @DisplayName("State-changing calls without the custom header are rejected (CSRF)")
    void csrfHeaderIsRequired() {
        String token = demoLogin();
        client().post().uri("/api/tasks").cookie(SessionTokens.COOKIE, token).exchange()
                .expectStatus().isForbidden();
        client().post().uri("/auth/demo").exchange().expectStatus().isForbidden();
        assertThat(received).isEmpty();
    }

    @Test
    void aTamperedCookieIsIgnored() {
        String token = demoLogin();
        String tampered = token.substring(0, token.length() - 3) + "abc";
        client().get().uri("/api/me").cookie(SessionTokens.COOKIE, tampered).exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void logoutClearsTheCookie() {
        ResponseCookie cookie = client().post().uri("/auth/logout").header("X-Requested-With", "luach").exchange()
                .expectStatus().isNoContent()
                .returnResult(Void.class).getResponseCookies().getFirst(SessionTokens.COOKIE);
        assertThat(cookie.getMaxAge().isZero()).isTrue();
    }

    @Test
    void reportsWhichLoginsAreAvailable() {
        client().get().uri("/auth/config").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.googleEnabled").isEqualTo(false);
    }

    @Test
    @DisplayName("App routes get index.html so the Angular router can handle deep links")
    void servesTheAppForDeepLinks() {
        client().get().uri("/join/some-token").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(body -> assertThat(body).contains("luach-app"));
        client().get().uri("/api/unknown").exchange().expectStatus().isUnauthorized();
    }

    private static HttpServer fakeService(String name) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", exchange -> {
                Map<String, String> headers = new ConcurrentHashMap<>();
                exchange.getRequestHeaders().forEach((k, v) -> headers.put(k.toLowerCase(), String.join(",", v)));
                received.put(name, headers);
                byte[] body = (name + ":" + exchange.getRequestURI().getPath()).getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
