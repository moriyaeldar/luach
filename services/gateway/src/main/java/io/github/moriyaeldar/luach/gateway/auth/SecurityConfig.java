package io.github.moriyaeldar.luach.gateway.auth;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationFailureHandler;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityWebFilterChain security(ServerHttpSecurity http, SessionTokens tokens,
                                    ObjectProvider<ReactiveClientRegistrationRepository> google) {
        http
                .securityContextRepository(new CookieSecurityContextRepository(tokens))
                // CSRF: the cookie is SameSite=Lax and every state-changing call must carry a custom header
                // (see RequireCustomHeaderFilter), which a cross-site form cannot send.
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .authorizeExchange(ex -> ex
                        .pathMatchers(HttpMethod.GET, "/api/invites/*").permitAll()
                        .pathMatchers("/api/**").authenticated()
                        .anyExchange().permitAll())
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(new RequireCustomHeaderFilter(), SecurityWebFiltersOrder.AUTHENTICATION);

        if (google.getIfAvailable() != null) {
            http.oauth2Login(login -> login
                    .authenticationSuccessHandler(new RedirectServerAuthenticationSuccessHandler("/"))
                    .authenticationFailureHandler(new RedirectServerAuthenticationFailureHandler("/?login=failed")));
        }
        return http.build();
    }

    /** Google login is optional: without a client id the app still runs, with demo login only. */
    @Configuration
    @ConditionalOnExpression("!'${luach.google.client-id:}'.isBlank()")
    static class GoogleLogin {
        @Bean
        ReactiveClientRegistrationRepository clientRegistrations(@Value("${luach.google.client-id}") String clientId,
                                                                 @Value("${luach.google.client-secret}") String secret) {
            return new InMemoryReactiveClientRegistrationRepository(CommonOAuth2Provider.GOOGLE.getBuilder("google")
                    .clientId(clientId)
                    .clientSecret(secret)
                    .scope("openid", "profile", "email")
                    .build());
        }
    }
}
