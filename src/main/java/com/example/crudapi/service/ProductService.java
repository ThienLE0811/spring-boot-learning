package com.example.crudapi.service;

import com.example.crudapi.dto.PageResponse;
import com.example.crudapi.dto.ProductRequest;
import com.example.crudapi.dto.ProductResponse;
import com.example.crudapi.entity.Category;
import com.example.crudapi.entity.Product;
import com.example.crudapi.exception.DuplicateResourceException;
import com.example.crudapi.exception.ResourceNotFoundException;
import com.example.crudapi.repository.CategoryRepository;
import com.example.crudapi.repository.ProductRepository;
import com.example.crudapi.repository.ProductSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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
    private final CategoryRepository categoryRepository;

    // Constructor injection: bat buoc phu thuoc, de test, khong can @Autowired.
    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    /**
     * Ca hai bo loc deu tuy chon. Bo loc vang mat tro thanh dieu kien luon dung
     * (xem ProductSpecifications), nen khong can phan nhanh theo tung to hop.
     *
     * categoryId khong ton tai -> tra ve trang rong chu khong phai 404: day la bo loc,
     * khong phai truy xuat resource.
     */
    public PageResponse<ProductResponse> search(String keyword, Long categoryId, Pageable pageable) {
        Specification<Product> spec = ProductSpecifications.keywordMatches(keyword)
                .and(ProductSpecifications.hasCategory(categoryId));

        Page<Product> page = productRepository.findAll(spec, pageable);

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
        product.setCategory(resolveCategory(request.categoryId()));
    }

    /**
     * Dung findById chu khong phai getReferenceById: getReferenceById chi tra ve proxy,
     * categoryId sai se chi lo ra thanh loi FK luc flush (500 kho hieu). findById cho phep
     * bao 404 ngay voi thong bao ro rang, doi lai mot cau SELECT.
     */
    private Category resolveCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay category voi id = " + categoryId));
    }
}
