package com.stie.service;

import com.stie.model.Department;
import com.stie.model.Tenant;
import com.stie.repository.DepartmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DepartmentService {

    @Autowired
    private DepartmentRepository departmentRepository;

    public List<Department> getDepartmentsByTenant(Tenant tenant) {
        return departmentRepository.findByTenantOrderByNameAsc(tenant);
    }

    public Optional<Department> getById(Long id) {
        return departmentRepository.findById(id);
    }

    public Department create(String name, Tenant tenant) {
        Department dept = new Department(name, tenant);
        return departmentRepository.save(dept);
    }

    public Department update(Long id, String name) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Department not found: " + id));
        dept.setName(name);
        return departmentRepository.save(dept);
    }

    public void delete(Long id) {
        departmentRepository.deleteById(id);
    }
}

