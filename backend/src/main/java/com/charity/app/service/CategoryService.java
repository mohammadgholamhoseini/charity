package com.charity.app.service;

import com.charity.app.model.Category;
import com.charity.app.payload.CategoryRequest;
import com.charity.app.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<Category> listActive() {
        return categoryRepository.findAll().stream().filter(Category::isActive).toList();
    }

    @Transactional(readOnly = true)
    public List<Category> listAll() {
        return categoryRepository.findAll();
    }

    @Transactional
    public Category create(CategoryRequest req) {
        Category c = Category.builder()
                .name(req.getName())
                .description(req.getDescription())
                .iconUrl(req.getIconUrl())
                .active(req.isActive())
                .build();
        return categoryRepository.save(c);
    }

    @Transactional
    public Category update(Long id, CategoryRequest req) {
        Category c = categoryRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("دسته‌بندی یافت نشد"));
        c.setName(req.getName());
        c.setDescription(req.getDescription());
        c.setIconUrl(req.getIconUrl());
        c.setActive(req.isActive());
        return categoryRepository.save(c);
    }

    @Transactional
    public void delete(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new NoSuchElementException("دسته‌بندی یافت نشد");
        }
        categoryRepository.deleteById(id);
    }
}
