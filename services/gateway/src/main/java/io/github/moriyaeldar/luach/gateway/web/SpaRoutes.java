package io.github.moriyaeldar.luach.gateway.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicate;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

/**
 * The Gateway also serves the Angular app (built into {@code classpath:/static}). Deep links such as /join/abc get
 * index.html so the Angular router can handle them. Same origin for app and API keeps the session cookie simple.
 */
@Configuration
public class SpaRoutes {

    private static final String[] BACKEND_PREFIXES = {"/api", "/auth", "/oauth2", "/login", "/actuator"};

    @Bean
    RouterFunction<ServerResponse> spaFallback() {
        Resource index = new ClassPathResource("static/index.html");
        RequestPredicate appRoute = GET("/**").and(request -> {
            String path = request.path();
            for (String prefix : BACKEND_PREFIXES) {
                if (path.equals(prefix) || path.startsWith(prefix + "/")) {
                    return false;
                }
            }
            return !path.equals("/") && !path.substring(path.lastIndexOf('/') + 1).contains(".");
        });
        return route(appRoute, request -> index.exists()
                ? ServerResponse.ok().contentType(MediaType.TEXT_HTML).bodyValue(index)
                : ServerResponse.notFound().build());
    }
}
