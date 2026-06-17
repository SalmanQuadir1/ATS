package com.stie.model;

import javax.persistence.*;

@Entity
@Table(name = "permission_modules")
public class PermissionModule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name; // e.g. "MANAGE_JOBS"

    @Column(columnDefinition = "TEXT")
    private String description; // e.g. "Create and approve jobs"

    public PermissionModule() {}

    public PermissionModule(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
