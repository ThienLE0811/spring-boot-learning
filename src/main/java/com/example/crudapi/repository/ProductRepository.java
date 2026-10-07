package com.example.crudapi.repository;

import com.example.crudapi.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Long id);

    boolean existsByCategoryId(Long categoryId);

    /**
     * Override chi de gan @EntityGraph.
     *
     * Product.category la LAZY, nen khi map N ban ghi sang ProductResponse, Hibernate
     * se ban them N cau SELECT category -> kinh dien N+1. @EntityGraph bien no thanh
     * mot LEFT JOIN FETCH duy nhat.
     *
     * Dung voi @ManyToOne nen van phan trang duoc o tang SQL (LIMIT/OFFSET). Luu y:
     * neu sau nay fetch mot collection (@OneToMany) thi Hibernate buoc phai phan trang
     * trong bo nho va se canh bao HHH90003004.
     */
    @Override
    @EntityGraph(attributePaths = "category")
    Page<Product> findAll(Specification<Product> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "category")
    Optional<Product> findById(Long id);
}
