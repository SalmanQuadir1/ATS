package com.stie.repository;

import com.stie.model.PermissionModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionModuleRepository extends JpaRepository<PermissionModule, Long> {
    Optional<PermissionModule> findByName(String name);
    List<PermissionModule> findByIsNavItemTrueOrderByNavOrder();
}
