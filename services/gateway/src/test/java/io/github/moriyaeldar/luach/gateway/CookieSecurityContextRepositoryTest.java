package io.github.moriyaeldar.luach.gateway;

import io.github.moriyaeldar.luach.gateway.auth.CookieSecurityContextRepository;
import io.github.moriyaeldar.luach.gateway.auth.SessionTokens;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CookieSecurityContextRepositoryTest {

    private final SessionTokens tokens = new SessionTokens("test-secret-that-is-long-enough-0123456789", true);
    private final CookieSecurityContextRepository repository = new CookieSecurityContextRepository(tokens);

    @Test
    void aGoogleLoginBecomesOurOwnSessionToken() {
        var idToken = new OidcIdToken("id-token", Instant.now(), Instant.now().plusSeconds(60),
                Map.of("sub", "1234", "name", "Moriya Eldar", "email", "moriya@example.com",
                        "picture", "https://example.com/me.png"));
        var user = new DefaultOidcUser(List.of(new SimpleGrantedAuthority("OIDC_USER")), idToken);
        var auth = new OAuth2AuthenticationToken(user, user.getAuthorities(), "google");
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/login/oauth2/code/google"));

        repository.save(exchange, new SecurityContextImpl(auth)).block();

        var cookie = exchange.getResponse().getCookies().getFirst(SessionTokens.COOKIE);
        assertThat(cookie).isNotNull();
        assertThat(cookie.isSecure()).isTrue();
        var jwt = tokens.decoder().decode(cookie.getValue()).block();
        assertThat(jwt.getSubject()).isEqualTo("google:1234");
        assertThat(jwt.getClaimAsString("email")).isEqualTo("moriya@example.com");
        assertThat(jwt.getClaimAsBoolean("demo")).isFalse();
    }

    @Test
    void loadsTheIdentityBackFromTheCookie() {
        String token = tokens.issue("google:1234", "Moriya", null, null, false);
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/me")
                .cookie(new org.springframework.http.HttpCookie(SessionTokens.COOKIE, token)));

        var context = repository.load(exchange).block();

        assertThat(context.getAuthentication().getName()).isEqualTo("google:1234");
    }
}
