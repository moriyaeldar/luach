package io.github.moriyaeldar.luach.household.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** The logged-in person, from the token the Gateway attaches to every request. */
public record Identity(String subject, String name, String email, String picture, boolean demo) {

    public static Identity current() {
        if (!(SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken token)) {
            throw new IllegalStateException("No authenticated user");
        }
        Jwt jwt = token.getToken();
        return new Identity(jwt.getSubject(), jwt.getClaimAsString("name"), jwt.getClaimAsString("email"),
                jwt.getClaimAsString("picture"), Boolean.TRUE.equals(jwt.getClaimAsBoolean("demo")));
    }
}
