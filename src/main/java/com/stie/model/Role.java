package com.stie.model;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "role_permissions", joinColumns = @JoinColumn(name = "role_id"))
    @Column(name = "permission")
    private Set<String> permissions = new HashSet<>();

    // Optional: if null, it's a global role available to all tenants, else it's specific
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;
    
    // Whether this role can be modified (e.g. system defaults like SUPER_ADMIN cannot be modified)
    private boolean isSystemRole = false;

    public Role() {}

    public Role(String name) {
        this.name = name;
    }

    public Role(String name, Set<String> permissions, boolean isSystemRole) {
        this.name = name;
        this.permissions = permissions;
        this.isSystemRole = isSystemRole;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Set<String> getPermissions() { return permissions; }
    public void setPermissions(Set<String> permissions) { this.permissions = permissions; }

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }

    public boolean isSystemRole() { return isSystemRole; }
    public void setSystemRole(boolean systemRole) { isSystemRole = systemRole; }
}
