package io.github.moriyaeldar.luach.tasks.members;

import io.github.moriyaeldar.luach.tasks.LuachProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Family members of the demo household.
 *
 * <p>Interim: moves to the Household service in milestone 2, together with Google login and invites.
 */
@RestController
public class MembersController {

    private final List<LuachProperties.DemoMember> members;

    public MembersController(LuachProperties properties) {
        this.members = properties.demoMembers();
    }

    @GetMapping("/api/members")
    public List<LuachProperties.DemoMember> members() {
        return members;
    }
}
