package io.github.moriyaeldar.luach.tasks;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param jwtSecret           shared secret for verifying user tokens and signing service-to-service calls
 * @param householdServiceUrl where to fetch a family that hasn't arrived through Kafka yet
 */
@ConfigurationProperties("luach")
public record LuachProperties(String jwtSecret, String householdServiceUrl) {
}
