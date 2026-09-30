package io.github.moriyaeldar.luach.gateway.auth;

import org.springframework.http.HttpCookie;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Keeps the security context in the session cookie instead of a server session.
 * <ul>
 *   <li>load: a valid cookie becomes a {@link JwtAuthenticationToken}; a missing or invalid one means anonymous</li>
 *   <li>save: after a successful Google login, issue our own token for the Google identity and set the cookie</li>
 * </ul>
 */
public class CookieSecurityContextRepository implements ServerSecurityContextRepository {

    private final SessionTokens tokens;

    public CookieSecurityContextRepository(SessionTokens tokens) {
        this.tokens = tokens;
    }

    @Override
    public Mono<Void> save(ServerWebExchange exchange, SecurityContext context) {
        if (context != null && context.getAuthentication() instanceof OAuth2AuthenticationToken oauth) {
            OAuth2User user = oauth.getPrincipal();
            String subject = oauth.getAuthorizedClientRegistrationId() + ":" + user.getName();
            String name = user.getAttribute("name");
            String email = user.getAttribute("email");
            String picture = user.getAttribute("picture");
            if (user instanceof OidcUser oidc) {
                subject = oauth.getAuthorizedClientRegistrationId() + ":" + oidc.getSubject();
            }
            String token = tokens.issue(subject, name, email, picture, false);
            exchange.getResponse().addCookie(tokens.cookie(token, false));
        }
        return Mono.empty();
    }

    @Override
    public Mono<SecurityContext> load(ServerWebExchange exchange) {
        HttpCookie cookie = exchange.getRequest().getCookies().getFirst(SessionTokens.COOKIE);
        if (cookie == null || cookie.getValue().isBlank()) {
            return Mono.empty();
        }
        return tokens.decoder().decode(cookie.getValue())
                .<SecurityContext>map(jwt -> new SecurityContextImpl(new JwtAuthenticationToken(jwt, java.util.List.of(), jwt.getSubject())))
                .onErrorResume(e -> Mono.empty());
    }
}
