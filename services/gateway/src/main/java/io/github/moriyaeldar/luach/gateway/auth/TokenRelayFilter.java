package io.github.moriyaeldar.luach.gateway.auth;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Forwards the session token to the services as {@code Authorization: Bearer ...} and never forwards browser cookies:
 * services only trust signed tokens.
 */
@Component
public class TokenRelayFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication() instanceof JwtAuthenticationToken jwt
                        ? jwt.getToken().getTokenValue() : "")
                .defaultIfEmpty("")
                .flatMap(token -> chain.filter(exchange.mutate().request(r -> r.headers(h -> {
                    h.remove(HttpHeaders.COOKIE);
                    h.remove(HttpHeaders.AUTHORIZATION);
                    if (!token.isEmpty()) {
                        h.setBearerAuth(token);
                    }
                })).build()));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
