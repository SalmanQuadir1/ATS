package com.stie.repository;

import com.stie.model.JobCategory;
import com.stie.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobCategoryRepository extends JpaRepository<JobCategory, Long> {
    List<JobCategory> findByTenantOrderByNameAsc(Tenant tenant);

    @Query("SELECT DISTINCT c FROM JobCategory c LEFT JOIN FETCH c.skills WHERE c.tenant = :tenant ORDER BY c.name ASC")
    List<JobCategory> findByTenantWithSkills(@Param("tenant") Tenant tenant);
}
