package io.github.moriyaeldar.luach.tasks.household;

import io.github.moriyaeldar.luach.events.HouseholdSnapshot;
import io.github.moriyaeldar.luach.tasks.LuachProperties;
import io.github.moriyaeldar.luach.tasks.security.ServiceTokens;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/** Calls the Household service's internal API with a short-lived service token. */
@Component
public class HouseholdClient {

    private final RestClient http;
    private final ServiceTokens tokens;

    public HouseholdClient(RestClient.Builder builder, LuachProperties properties, ServiceTokens tokens) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        // Generous: on free hosting the Household service may be waking up.
        factory.setReadTimeout(Duration.ofSeconds(90));
        this.http = builder.baseUrl(properties.householdServiceUrl()).requestFactory(factory).build();
        this.tokens = tokens;
    }

    public Optional<HouseholdSnapshot> fetch(UUID householdId) {
        try {
            return Optional.ofNullable(http.get()
                    .uri("/internal/households/{id}", householdId)
                    .headers(h -> h.setBearerAuth(tokens.internalToken()))
                    .retrieve()
                    .body(HouseholdSnapshot.class));
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }
            throw e;
        }
    }
}
