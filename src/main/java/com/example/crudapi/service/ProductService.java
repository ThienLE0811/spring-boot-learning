package com.example.crudapi.service;

import com.example.crudapi.dto.PageResponse;
import com.example.crudapi.dto.ProductRequest;
import com.example.crudapi.dto.ProductResponse;
import com.example.crudapi.entity.Product;
import com.example.crudapi.exception.DuplicateResourceException;
import com.example.crudapi.exception.ResourceNotFoundException;
import com.example.crudapi.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Chua toan bo business logic. Controller chi lam nhiem vu dieu huong HTTP,
 * repository chi lam nhiem vu truy van DB.
 */
@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    // Constructor injection: bat buoc phu thuoc, de test, khong can @Autowired.
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public PageResponse<ProductResponse> search(String keyword, Pageable pageable) {
        String normalized = StringUtils.hasText(keyword) ? keyword.trim() : null;

        Page<Product> page = (normalized == null)
                ? productRepository.findAll(pageable)
                : productRepository.findByNameContainingIgnoreCaseOrSkuContainingIgnoreCase(
                        normalized, normalized, pageable);

        return PageResponse.of(page, ProductResponse::from);
    }

    public ProductResponse getById(Long id) {
        return ProductResponse.from(findOrThrow(id));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        String sku = request.sku().trim();
        if (productRepository.existsBySku(sku)) {
            throw new DuplicateResourceException("SKU da ton tai: " + sku);
        }

        Product product = new Product();
        product.setSku(sku);
        applyRequest(product, request);

        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = findOrThrow(id);

        String sku = request.sku().trim();
        if (productRepository.existsBySkuAndIdNot(sku, id)) {
            throw new DuplicateResourceException("SKU da ton tai: " + sku);
        }

        product.setSku(sku);
        applyRequest(product, request);

        // Khong can goi save(): product dang o trang thai managed trong persistence
        // context, Hibernate se tu flush khi transaction commit.
        return ProductResponse.from(product);
    }

    @Transactional
    public void delete(Long id) {
        Product product = findOrThrow(id);
        productRepository.delete(product);
    }

    private Product findOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay product voi id = " + id));
    }

    private void applyRequest(Product product, ProductRequest request) {
        product.setName(request.name().trim());
        product.setDescription(StringUtils.hasText(request.description()) ? request.description().trim() : null);
        product.setPrice(request.price());
        product.setQuantity(request.quantity());
    }
}
