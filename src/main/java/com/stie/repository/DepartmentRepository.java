package com.stie.repository;

import com.stie.model.Department;
import com.stie.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    List<Department> findByTenantOrderByNameAsc(Tenant tenant);
}

