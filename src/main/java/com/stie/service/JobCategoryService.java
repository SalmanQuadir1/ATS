package com.stie.service;

import com.stie.model.JobCategory;
import com.stie.model.Skill;
import com.stie.model.Tenant;
import com.stie.repository.JobCategoryRepository;
import com.stie.repository.SkillRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class JobCategoryService {

    @Autowired
    private JobCategoryRepository categoryRepository;

    @Autowired
    private SkillRepository skillRepository;

    public List<JobCategory> getCategoriesByTenant(Tenant tenant) {
        return categoryRepository.findByTenantOrderByNameAsc(tenant);
    }

    public Optional<JobCategory> getCategoryById(Long id) {
        return categoryRepository.findById(id);
    }

    public JobCategory createCategory(String name, Tenant tenant) {
        JobCategory category = new JobCategory(name, tenant);
        return categoryRepository.save(category);
    }

    public JobCategory updateCategory(Long id, String newName) {
        Optional<JobCategory> opt = categoryRepository.findById(id);
        if (opt.isPresent()) {
            JobCategory category = opt.get();
            category.setName(newName);
            return categoryRepository.save(category);
        }
        return null;
    }

    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }

    public List<Skill> getSkillsByCategory(JobCategory category) {
        return skillRepository.findByCategoryOrderByNameAsc(category);
    }

    public Skill addSkill(String name, JobCategory category) {
        Skill skill = new Skill(name, category);
        return skillRepository.save(skill);
    }

    public Skill updateSkill(Long id, String newName) {
        Optional<Skill> opt = skillRepository.findById(id);
        if (opt.isPresent()) {
            Skill skill = opt.get();
            skill.setName(newName);
            return skillRepository.save(skill);
        }
        return null;
    }

    public void deleteSkill(Long id) {
        skillRepository.deleteById(id);
    }
}

