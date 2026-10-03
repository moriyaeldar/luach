package io.github.moriyaeldar.luach.gateway.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

/**
 * Issues and reads the session token. The same signed JWT lives in an HttpOnly cookie in the browser and is forwarded
 * to the services as a bearer token, so no server-side session is needed (the Gateway can restart or scale freely).
 */
@Component
public class SessionTokens {

    public static final String COOKIE = "luach_session";
    static final String ISSUER = "luach-gateway";
    static final Duration USER_TTL = Duration.ofDays(7);
    static final Duration DEMO_TTL = Duration.ofHours(24);

    private final JwtEncoder encoder;
    private final ReactiveJwtDecoder decoder;
    private final boolean secureCookie;

    public SessionTokens(@Value("${luach.jwt-secret}") String secret,
                         @Value("${luach.cookie.secure:true}") boolean secureCookie) {
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("luach.jwt-secret must be at least 32 bytes");
        }
        SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.encoder = NimbusJwtEncoder.withSecretKey(key).algorithm(MacAlgorithm.HS256).build();
        this.decoder = NimbusReactiveJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        this.secureCookie = secureCookie;
    }

    public String issue(String subject, String name, String email, String picture, boolean demo) {
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(subject)
                .issuedAt(now)
                .expiresAt(now.plus(demo ? DEMO_TTL : USER_TTL))
                .claim("name", name == null ? "" : name)
                .claim("demo", demo);
        if (email != null) {
            claims.claim("email", email);
        }
        if (picture != null) {
            claims.claim("picture", picture);
        }
        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
    }

    public ReactiveJwtDecoder decoder() {
        return decoder;
    }

    public ResponseCookie cookie(String token, boolean demo) {
        return ResponseCookie.from(COOKIE, token)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path("/")
                .maxAge(demo ? DEMO_TTL : USER_TTL)
                .build();
    }

    public ResponseCookie expiredCookie() {
        return ResponseCookie.from(COOKIE, "").httpOnly(true).secure(secureCookie).sameSite("Lax").path("/")
                .maxAge(Duration.ZERO).build();
    }
}
