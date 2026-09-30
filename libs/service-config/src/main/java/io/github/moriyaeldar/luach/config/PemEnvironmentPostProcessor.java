package io.github.moriyaeldar.luach.config;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Normalizes PEM certificates and keys passed in environment variables (the managed Kafka client certificates).
 * Hosting dashboards don't all keep line breaks, so each variable may hold the PEM text as-is,
 * with literal {@code \n} escapes, or base64-encoded ({@code base64 -w0 ca.pem}).
 */
public class PemEnvironmentPostProcessor implements EnvironmentPostProcessor {

    static final String SOURCE_NAME = "pemVariables";
    static final List<String> VARIABLES = List.of("KAFKA_CA_CERT", "KAFKA_ACCESS_CERT", "KAFKA_ACCESS_KEY");

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> normalized = new HashMap<>();
        for (String name : VARIABLES) {
            String value = environment.getProperty(name);
            if (value != null && !value.isBlank()) {
                String pem = toPem(value);
                if (!pem.equals(value)) {
                    normalized.put(name, pem);
                }
            }
        }
        if (!normalized.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource(SOURCE_NAME, normalized));
        }
    }

    static String toPem(String value) {
        String trimmed = value.replace("\\r\\n", "\n").replace("\\n", "\n").replace("\r\n", "\n").trim();
        if (!trimmed.contains("-----BEGIN")) {
            try {
                trimmed = new String(Base64.getMimeDecoder().decode(trimmed), StandardCharsets.UTF_8).trim();
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Expected a PEM value or base64-encoded PEM", e);
            }
            if (!trimmed.contains("-----BEGIN")) {
                throw new IllegalArgumentException("Expected a PEM value or base64-encoded PEM");
            }
        }
        return trimmed.replace("\r\n", "\n") + "\n";
    }
}
