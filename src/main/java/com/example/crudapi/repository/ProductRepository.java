package com.example.crudapi.repository;

import com.example.crudapi.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Long id);

    /**
     * Tim theo ten hoac sku, khong phan biet hoa thuong.
     *
     * Dung derived query thay vi JPQL dang "WHERE :keyword IS NULL OR ...": PostgreSQL
     * khong suy duoc kieu cua tham so trong bieu thuc "? IS NULL" va bao loi
     * "could not determine data type of parameter". Truong hop khong loc duoc xu ly
     * bang findAll() o tang service.
     */
    Page<Product> findByNameContainingIgnoreCaseOrSkuContainingIgnoreCase(
            String name, String sku, Pageable pageable);
}
