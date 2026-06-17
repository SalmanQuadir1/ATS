package com.stie.model;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    private String displayName; // Full name for display

    private String email;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "tenant_id")
    private Tenant tenant; // Which Site this user belongs to (null for SuperAdmin)

    public User() {
    }

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public User(String username, String password, String placeholder) {
        this.username = username;
        this.password = password;
        // The placeholder parameter is just for backwards-compatibility of the constructor signature
        // if needed, otherwise roles should be managed via getRoles().add()
    }

    public User(String username, String password, String placeholder, Tenant tenant, String displayName, String email) {
        this.username = username;
        this.password = password;
        this.tenant = tenant;
        this.displayName = displayName;
        this.email = email;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    // Helper method to keep UI compatibility where it expects a single role string
    public String getRole() {
        if (roles != null && !roles.isEmpty()) {
            return roles.iterator().next().getName();
        }
        return "";
    }

    public void setRole(String roleName) {
        // Dummy setter for form binding if needed, though forms should bind to role IDs ideally
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }
}
