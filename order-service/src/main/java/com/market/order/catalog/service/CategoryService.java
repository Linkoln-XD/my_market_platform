package com.market.order.catalog.service;

import com.market.order.catalog.CatalogConflictException;
import com.market.order.catalog.CatalogNotFoundException;
import com.market.order.catalog.cache.CatalogCacheNames;
import com.market.order.catalog.domain.CategoryEntity;
import com.market.order.catalog.dto.CategoryResponse;
import com.market.order.catalog.dto.CreateCategoryRequest;
import com.market.order.catalog.mapper.CatalogMapper;
import com.market.order.catalog.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CatalogMapper catalogMapper;

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CatalogCacheNames.CATEGORIES)
    public List<CategoryResponse> listCategories() {
        return categoryRepository.findAll().stream()
                .map(catalogMapper::toCategoryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CatalogCacheNames.CATEGORY_BY_ID, key = "#id")
    public CategoryResponse getCategory(UUID id) {
        return categoryRepository.findById(id)
                .map(catalogMapper::toCategoryResponse)
                .orElseThrow(() -> new CatalogNotFoundException("Category not found: " + id));
    }

    @Transactional
    @CacheEvict(cacheNames = {CatalogCacheNames.CATEGORIES, CatalogCacheNames.CATEGORY_BY_ID}, allEntries = true)
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        if (categoryRepository.existsBySlug(request.slug())) {
            throw new CatalogConflictException("Category slug already exists: " + request.slug());
        }
        CategoryEntity entity = new CategoryEntity();
        entity.setName(request.name());
        entity.setSlug(request.slug());
        return catalogMapper.toCategoryResponse(categoryRepository.save(entity));
    }
}
