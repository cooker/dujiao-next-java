package com.dujiao.api.service;

import com.dujiao.api.common.api.ResponseCodes;
import com.dujiao.api.common.exception.BusinessException;
import com.dujiao.api.domain.Category;
import com.dujiao.api.dto.category.AdminCategoryDto;
import com.dujiao.api.dto.category.CategoryUpsertRequest;
import com.dujiao.api.repository.CategoryRepository;
import com.dujiao.api.repository.ProductRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminCategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;

    public AdminCategoryService(
            CategoryRepository categoryRepository,
            ProductRepository productRepository,
            ObjectMapper objectMapper) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<AdminCategoryDto> list() {
        return categoryRepository.findAllByOrderBySortOrderAsc().stream().map(this::toDto).toList();
    }

    @Transactional
    public AdminCategoryDto create(CategoryUpsertRequest req) {
        if (categoryRepository.findBySlug(req.slug().trim()).isPresent()) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "slug_exists");
        }
        long parentId = req.parentId() == null ? 0L : req.parentId();
        validateParent(parentId, null);
        Category c = new Category();
        applyCreate(c, req);
        c = categoryRepository.save(c);
        return toDto(c);
    }

    @Transactional
    public AdminCategoryDto update(long id, CategoryUpsertRequest req) {
        Category c =
                categoryRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ResponseCodes.NOT_FOUND, "category_not_found"));
        String slug = req.slug().trim();
        categoryRepository
                .findBySlug(slug)
                .filter(x -> !x.getId().equals(id))
                .ifPresent(x -> {
                    throw new BusinessException(ResponseCodes.BAD_REQUEST, "slug_used");
                });
        long parentId = req.parentId() == null ? 0L : req.parentId();
        validateParent(parentId, id);
        applyUpdate(c, req);
        return toDto(categoryRepository.save(c));
    }

    @Transactional
    public void delete(long id) {
        if (!categoryRepository.existsById(id)) {
            throw new BusinessException(ResponseCodes.NOT_FOUND, "category_not_found");
        }
        if (productRepository.existsByCategoryId(id)) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "category_in_use");
        }
        categoryRepository.deleteById(id);
    }

    private void applyCreate(Category c, CategoryUpsertRequest req) {
        long parentId = req.parentId() == null ? 0L : req.parentId();
        c.setParentId(Math.max(parentId, 0L));
        c.setSlug(req.slug().trim());
        c.setNameJson(writeNameJson(req.name()));
        c.setName(extractDefaultName(req.name()));
        c.setIcon(req.icon() == null ? "" : req.icon().trim());
        c.setSortOrder(req.sortOrder() != null ? req.sortOrder() : 0);
    }

    private void applyUpdate(Category c, CategoryUpsertRequest req) {
        long parentId = req.parentId() == null ? 0L : req.parentId();
        c.setParentId(Math.max(parentId, 0L));
        c.setSlug(req.slug().trim());
        c.setNameJson(writeNameJson(req.name()));
        c.setName(extractDefaultName(req.name()));
        c.setIcon(req.icon() == null ? "" : req.icon().trim());
        if (req.sortOrder() != null) {
            c.setSortOrder(req.sortOrder());
        }
    }

    private AdminCategoryDto toDto(Category c) {
        return new AdminCategoryDto(
                c.getId(),
                c.getParentId(),
                c.getSlug(),
                readNameJson(c),
                c.getIcon() == null ? "" : c.getIcon(),
                c.getSortOrder(),
                c.getCreatedAt());
    }

    private void validateParent(long parentId, Long selfId) {
        if (parentId <= 0) {
            return;
        }
        if (selfId != null && parentId == selfId) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "category_parent_invalid");
        }
        if (!categoryRepository.existsById(parentId)) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "category_parent_invalid");
        }
    }

    private String extractDefaultName(Map<String, Object> nameJson) {
        Object zh = nameJson.get("zh-CN");
        if (zh != null && !String.valueOf(zh).isBlank()) {
            return String.valueOf(zh).trim();
        }
        return nameJson.values().stream()
                .filter(v -> v != null && !String.valueOf(v).isBlank())
                .map(v -> String.valueOf(v).trim())
                .findFirst()
                .orElse("");
    }

    private String writeNameJson(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "bad_request");
        }
    }

    private Map<String, Object> readNameJson(Category c) {
        if (c.getNameJson() == null || c.getNameJson().isBlank()) {
            return Map.of("zh-CN", c.getName() == null ? "" : c.getName());
        }
        try {
            return objectMapper.readValue(c.getNameJson(), new TypeReference<Map<String, Object>>() {});
        } catch (Exception ignored) {
            return Map.of("zh-CN", c.getName() == null ? "" : c.getName());
        }
    }
}
