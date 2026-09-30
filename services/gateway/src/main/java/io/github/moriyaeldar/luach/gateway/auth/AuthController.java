package io.github.moriyaeldar.luach.gateway.auth;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import java.util.Map;
import java.util.UUID;

/** Login endpoints that aren't part of the OAuth flow. */
@RestController
public class AuthController {

    private final SessionTokens tokens;
    private final boolean googleEnabled;

    public AuthController(SessionTokens tokens, ObjectProvider<ReactiveClientRegistrationRepository> google) {
        this.tokens = tokens;
        this.googleEnabled = google.getIfAvailable() != null;
    }

    /** Tells the app which login options to show. */
    @GetMapping("/auth/config")
    public Map<String, Object> config() {
        return Map.of("googleEnabled", googleEnabled, "googleLoginUrl", "/oauth2/authorization/google");
    }

    /** "Try the demo": a fresh anonymous identity that gets its own sample family, valid for a day. */
    @PostMapping("/auth/demo")
    public ResponseEntity<Void> demo(ServerWebExchange exchange) {
        String token = tokens.issue("demo:" + UUID.randomUUID(), "Demo", null, null, true);
        exchange.getResponse().addCookie(tokens.cookie(token, true));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(ServerWebExchange exchange) {
        exchange.getResponse().addCookie(tokens.expiredCookie());
        return ResponseEntity.noContent().build();
    }
}
