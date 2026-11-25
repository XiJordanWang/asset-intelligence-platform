package io.github.xijordanwang.assetintel.security.entity;

import jakarta.persistence.*;

import java.util.EnumSet;
import java.util.Set;

@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(nullable = false)
    private String title;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "plant_code")
    private String plantCode;

    @Column(nullable = false)
    private boolean enabled = true;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "app_user_role", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    private Set<UserRole> roles = EnumSet.noneOf(UserRole.class);

    @Version
    private long version;

    protected AppUser() {
    }

    public AppUser(String username, String displayName, String title, String passwordHash, String plantCode,
                   Set<UserRole> roles) {
        this.username = username;
        this.displayName = displayName;
        this.title = title;
        this.passwordHash = passwordHash;
        this.plantCode = plantCode;
        this.roles = roles.isEmpty() ? EnumSet.noneOf(UserRole.class) : EnumSet.copyOf(roles);
    }

    public void replaceRoles(Set<UserRole> newRoles) {
        roles.clear();
        roles.addAll(newRoles);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getDisplayName() { return displayName; }
    public String getTitle() { return title; }
    public String getPasswordHash() { return passwordHash; }
    public String getPlantCode() { return plantCode; }
    public boolean isEnabled() { return enabled; }
    public Set<UserRole> getRoles() { return Set.copyOf(roles); }
}
