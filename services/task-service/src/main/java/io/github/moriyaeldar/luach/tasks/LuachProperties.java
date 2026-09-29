package io.github.moriyaeldar.luach.tasks;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.UUID;

@ConfigurationProperties("luach")
public record LuachProperties(Household household, List<DemoMember> demoMembers) {

    public record Household(UUID id, boolean inIsrael) {
    }

    /** Temporary family members until the Household service is introduced (milestone 2). */
    public record DemoMember(String id, String he, String en, String color) {
    }

    public LuachProperties {
        demoMembers = demoMembers == null ? List.of() : List.copyOf(demoMembers);
    }
}
