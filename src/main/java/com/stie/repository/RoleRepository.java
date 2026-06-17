package com.stie.repository;

import com.stie.model.Role;
import com.stie.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    List<Role> findByNameAndTenantIsNull(String name);
    List<Role> findByTenantIsNull();
    List<Role> findByTenant(Tenant tenant);
    List<Role> findByTenantOrTenantIsNull(Tenant tenant);
}
