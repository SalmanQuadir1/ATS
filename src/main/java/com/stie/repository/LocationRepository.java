package com.stie.repository;

import com.stie.model.Location;
import com.stie.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {
    List<Location> findByTenantOrderByNameAsc(Tenant tenant);
}

