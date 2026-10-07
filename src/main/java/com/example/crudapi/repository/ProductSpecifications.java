package com.example.crudapi.repository;

import com.example.crudapi.entity.Product;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/**
 * Cac dieu kien loc cho Product, ghep duoc voi nhau bang Specification.and().
 *
 * Vi sao khong dung derived query: moi bo loc tuy chon lam so to hop nhan doi
 * (keyword x categoryId = 4 truong hop). Dung derived query se phai viet 4 method
 * va 4 nhanh if o service; them mot bo loc nua thanh 8. Specification cho phep
 * ghep dong, so method khong tang theo to hop.
 *
 * Bo loc vang mat tra ve Specification.unrestricted() (dieu kien luon dung) thay vi
 * null, de phia goi khong phai kiem tra null truoc khi and().
 */
public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    /** Tim theo name hoac sku, khong phan biet hoa thuong. */
    public static Specification<Product> keywordMatches(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return Specification.unrestricted();
        }

        String pattern = "%" + keyword.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("sku")), pattern));
    }

    /**
     * Loc theo category.
     *
     * So sanh truc tiep voi khoa ngoai (root.get("category").get("id")) nen Hibernate
     * dung luon cot products.category_id, khong phai join them bang categories.
     */
    public static Specification<Product> hasCategory(Long categoryId) {
        if (categoryId == null) {
            return Specification.unrestricted();
        }

        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }
}
