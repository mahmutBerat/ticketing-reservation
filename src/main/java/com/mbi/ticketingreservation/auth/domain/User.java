package com.mbi.ticketingreservation.auth.domain;

import com.mbi.ticketingreservation.common.persistence.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "app_users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_id_generator")
    @SequenceGenerator(name = "user_id_generator", sequenceName = "app_users_seq")
    private Long id;

    @Column(nullable = false, length = 320, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private Set<Role> roles = EnumSet.noneOf(Role.class);

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    public User(String email, String passwordHash, Set<Role> roles) {
        this.email = requireText(email, "email").toLowerCase();
        this.passwordHash = requireText(passwordHash, "passwordHash");
        Objects.requireNonNull(roles, "roles must not be null");
        if (roles.isEmpty()) {
            throw new IllegalArgumentException("roles must not be empty");
        }
        this.roles = EnumSet.copyOf(roles);
    }

    public void recordLogin(Instant loginTime) {
        lastLoginAt = Objects.requireNonNull(loginTime, "loginTime must not be null");
    }

    public Set<Role> getRoles() {
        return Collections.unmodifiableSet(roles);
    }

    public void replaceRoles(Set<Role> roles) {
        Objects.requireNonNull(roles, "roles must not be null");
        if (roles.isEmpty()) {
            throw new IllegalArgumentException("roles must not be empty");
        }
        if (this.roles.contains(Role.ADMIN)) {
            throw new AdminRolesImmutableException();
        }
        this.roles = EnumSet.copyOf(roles);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
