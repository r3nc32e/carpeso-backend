package com.carpeso.carpeso_backend.service;

import com.carpeso.carpeso_backend.model.Category;
import com.carpeso.carpeso_backend.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private AuditLogService auditLogService;

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Category addCategory(String name, String performedBy) {
        if (name == null || name.trim().isEmpty()) {
            throw new RuntimeException("Category name is required!");
        }
        if (categoryRepository.existsByName(name.trim())) {
            throw new RuntimeException("Category already exists!");
        }
        Category category = new Category();
        category.setName(name.trim());
        categoryRepository.save(category);
        auditLogService.log("CATEGORY_ADDED", performedBy,
                "Category", String.valueOf(category.getId()),
                "Added category: " + name, "system");
        return category;
    }

    public void deleteCategory(Long id, String performedBy) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found!"));
        categoryRepository.deleteById(id);
        auditLogService.log("CATEGORY_DELETED", performedBy,
                "Category", String.valueOf(id),
                "Deleted category: " + category.getName(), "system");
    }
}