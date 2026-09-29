package io.github.moriyaeldar.luach.gateway;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/** Starts a tiny fake task-service and checks that the gateway forwards /api requests to it. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRoutingTest {

    private static final HttpServer fakeTaskService = startFakeTaskService();

    @LocalServerPort
    private int port;

    @DynamicPropertySource
    static void routes(DynamicPropertyRegistry registry) {
        registry.add("TASK_SERVICE_URL", () -> "http://localhost:" + fakeTaskService.getAddress().getPort());
    }

    @AfterAll
    static void stop() {
        fakeTaskService.stop(0);
    }

    @Test
    void forwardsWeekRequestsToTheTaskService() {
        client().get().uri("/api/week?start=2026-10-04").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("task-service:/api/week");
    }

    @Test
    void forwardsTaskRequests() {
        client().get().uri("/api/tasks/123").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("task-service:/api/tasks/123");
    }

    @Test
    void unknownPathsAreNotRouted() {
        client().get().uri("/api/unknown").exchange().expectStatus().isNotFound();
    }

    private WebTestClient client() {
        return WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    private static HttpServer startFakeTaskService() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", exchange -> {
                byte[] body = ("task-service:" + exchange.getRequestURI().getPath()).getBytes(StandardCharsets.UTF_8);
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
