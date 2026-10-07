package com.example.crudapi.service;

import com.example.crudapi.dto.CategoryRequest;
import com.example.crudapi.dto.CategoryResponse;
import com.example.crudapi.dto.PageResponse;
import com.example.crudapi.entity.Category;
import com.example.crudapi.exception.DuplicateResourceException;
import com.example.crudapi.exception.ResourceInUseException;
import com.example.crudapi.exception.ResourceNotFoundException;
import com.example.crudapi.repository.CategoryRepository;
import com.example.crudapi.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryService(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    public PageResponse<CategoryResponse> search(String keyword, Pageable pageable) {
        String normalized = StringUtils.hasText(keyword) ? keyword.trim() : null;

        Page<Category> page = (normalized == null)
                ? categoryRepository.findAll(pageable)
                : categoryRepository.findByNameContainingIgnoreCase(normalized, pageable);

        return PageResponse.of(page, CategoryResponse::from);
    }

    public CategoryResponse getById(Long id) {
        return CategoryResponse.from(findOrThrow(id));
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String name = request.name().trim();
        if (categoryRepository.existsByName(name)) {
            throw new DuplicateResourceException("Ten category da ton tai: " + name);
        }

        Category category = new Category();
        applyRequest(category, request);

        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = findOrThrow(id);

        String name = request.name().trim();
        if (categoryRepository.existsByNameAndIdNot(name, id)) {
            throw new DuplicateResourceException("Ten category da ton tai: " + name);
        }

        applyRequest(category, request);

        // Entity dang managed -> Hibernate tu flush khi commit, khong can save().
        return CategoryResponse.from(category);
    }

    /**
     * Khong xoa category con product tham chieu toi.
     *
     * FK o DB da chan truong hop nay roi, nhung neu de no nem len thanh
     * DataIntegrityViolationException thi client chi nhan duoc thong bao chung chung.
     * Kiem tra truoc o day de tra ve 409 kem ly do cu the.
     */
    @Transactional
    public void delete(Long id) {
        Category category = findOrThrow(id);

        if (productRepository.existsByCategoryId(id)) {
            throw new ResourceInUseException(
                    "Khong the xoa category dang duoc product su dung: " + category.getName());
        }

        categoryRepository.delete(category);
    }

    private Category findOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay category voi id = " + id));
    }

    private void applyRequest(Category category, CategoryRequest request) {
        category.setName(request.name().trim());
        category.setDescription(StringUtils.hasText(request.description()) ? request.description().trim() : null);
    }
}
