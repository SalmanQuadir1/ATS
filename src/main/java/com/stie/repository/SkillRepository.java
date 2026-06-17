package com.stie.repository;

import com.stie.model.JobCategory;
import com.stie.model.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {
    List<Skill> findByCategoryOrderByNameAsc(JobCategory category);
}

