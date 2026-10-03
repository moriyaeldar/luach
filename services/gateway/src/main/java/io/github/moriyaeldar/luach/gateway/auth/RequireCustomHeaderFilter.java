package io.github.moriyaeldar.luach.gateway.auth;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Set;

/**
 * CSRF defence for a cookie-authenticated SPA: state-changing calls to our API must carry {@code X-Requested-With}.
 * Browsers only let pages on our own origin add custom headers, so a forged cross-site request is rejected.
 */
public class RequireCustomHeaderFilter implements WebFilter {

    static final String HEADER = "X-Requested-With";
    private static final Set<HttpMethod> SAFE = Set.of(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        var request = exchange.getRequest();
        String path = request.getPath().value();
        boolean protectedPath = path.startsWith("/api/") || path.startsWith("/auth/");
        if (protectedPath && !SAFE.contains(request.getMethod()) && !request.getHeaders().containsHeader(HEADER)) {
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
        }
        return chain.filter(exchange);
    }
}
