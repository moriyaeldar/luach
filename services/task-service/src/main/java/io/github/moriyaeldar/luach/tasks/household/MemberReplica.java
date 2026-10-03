package io.github.moriyaeldar.luach.tasks.household;

import io.github.moriyaeldar.luach.events.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "member_replica")
public class MemberReplica {

    @Id
    private UUID id;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(nullable = false)
    private String color;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "user_subject")
    private String userSubject;

    protected MemberReplica() {
    }

    public MemberReplica(UUID id, String displayName, String color, Role role, String userSubject) {
        this.id = id;
        this.displayName = displayName;
        this.color = color;
        this.role = role;
        this.userSubject = userSubject;
    }

    void copyFrom(MemberReplica other) {
        this.displayName = other.displayName;
        this.color = other.color;
        this.role = other.role;
        this.userSubject = other.userSubject;
    }

    public UUID getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColor() {
        return color;
    }

    public Role getRole() {
        return role;
    }

    public String getUserSubject() {
        return userSubject;
    }
}
