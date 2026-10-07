package com.example.crudapi.repository;

import com.example.crudapi.config.JpaAuditingConfig;
import com.example.crudapi.entity.Category;
import com.example.crudapi.entity.Product;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test chay tren Postgres that (Testcontainers) thay vi doan Hibernate
 * tao SQL gi roi doi chieu bang tay voi profile dev.
 *
 * - Flyway migrate that len container nay: entity lech schema (ddl-auto=validate)
 *   se fail ngay o day.
 * - Dung Hibernate Statistics de dem so cau SQL thuc thi, chung minh @EntityGraph
 *   tren ProductRepository gop lazy-load category lai, khong con N+1.
 * - Chay Specification qua EntityManager that de kiem ket qua loc dung, bo sung cho
 *   ProductSpecificationsTest (test do chi mock Criteria API, khong cham DB that).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Testcontainers
class ProductRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Category keyboardCategory;

    @BeforeEach
    void setUp() {
        keyboardCategory = categoryRepository.save(category("Ban phim & Chuot"));
        Category cableCategory = categoryRepository.save(category("Phu kien"));

        productRepository.save(product("SKU-KB01", "Ban phim co", keyboardCategory));
        productRepository.save(product("SKU-MS01", "Chuot khong day", keyboardCategory));
        productRepository.save(product("SKU-CB01", "Cap sac USB-C", cableCategory));
        productRepository.save(product("SKU-HB01", "Hub USB", null));

        // Clear persistence context sau khi seed du lieu, gia lap mot request moi:
        // cac query ben duoi khong con duoc "an" boi first-level cache cua chinh buoc seed.
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("findAll: @EntityGraph gop lazy-load category lai, khong no N+1 theo so product")
    void findAll_shouldNotTriggerNPlusOneQueries() {
        Statistics statistics = statistics();
        statistics.clear();

        Page<Product> page = productRepository.findAll(Specification.unrestricted(),
                PageRequest.of(0, 10, Sort.by("id")));

        // Truy cap category cua tung product, dung nhu luc ProductService map sang ProductResponse.
        // Khong co @EntityGraph: moi lan .getCategory().getName() se ban them 1 SELECT rieng.
        page.getContent().forEach(p -> {
            if (p.getCategory() != null) {
                assertThat(p.getCategory().getName()).isNotBlank();
            }
        });

        // 1 cau LEFT JOIN FETCH cho noi dung trang; Spring Data co the bo qua COUNT query
        // vi noi dung (4 dong) chua day page size (10). Neu khong co @EntityGraph, so nay
        // se la it nhat 1 + 3 (so product co category) = 4.
        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(2);
    }

    @Test
    @DisplayName("findById: @EntityGraph fetch category trong dung 1 cau SQL")
    void findById_shouldFetchCategoryInSingleQuery() {
        Long id = productRepository.findBySku("SKU-KB01").orElseThrow().getId();
        entityManager.clear();

        Statistics statistics = statistics();
        statistics.clear();

        Product product = productRepository.findById(id).orElseThrow();
        assertThat(product.getCategory().getName()).isEqualTo("Ban phim & Chuot");

        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Specification: keywordMatches khop ca name lan sku, khong phan biet hoa thuong")
    void search_byKeyword_shouldMatchNameOrSku() {
        Page<Product> result = productRepository.findAll(
                ProductSpecifications.keywordMatches("usb"),
                PageRequest.of(0, 10, Sort.by("sku")));

        assertThat(result.getContent())
                .extracting(Product::getSku)
                .containsExactlyInAnyOrder("SKU-CB01", "SKU-HB01");
    }

    @Test
    @DisplayName("Specification: hasCategory chi tra ve dung product cua category do")
    void search_byCategory_shouldReturnOnlyProductsOfThatCategory() {
        Page<Product> result = productRepository.findAll(
                ProductSpecifications.hasCategory(keyboardCategory.getId()),
                PageRequest.of(0, 10, Sort.by("sku")));

        assertThat(result.getContent())
                .extracting(Product::getSku)
                .containsExactlyInAnyOrder("SKU-KB01", "SKU-MS01");
    }

    @Test
    @DisplayName("Specification: ket hop keyword.and(hasCategory) thu hep dung giao cua 2 dieu kien")
    void search_byKeywordAndCategory_shouldIntersectBothConditions() {
        Page<Product> result = productRepository.findAll(
                ProductSpecifications.keywordMatches("khong day")
                        .and(ProductSpecifications.hasCategory(keyboardCategory.getId())),
                PageRequest.of(0, 10));

        assertThat(result.getContent())
                .extracting(Product::getSku)
                .containsExactly("SKU-MS01");
    }

    private Statistics statistics() {
        return entityManager.getEntityManager()
                .getEntityManagerFactory()
                .unwrap(SessionFactory.class)
                .getStatistics();
    }

    private static Category category(String name) {
        Category category = new Category();
        category.setName(name);
        return category;
    }

    private static Product product(String sku, String name, Category category) {
        Product product = new Product();
        product.setSku(sku);
        product.setName(name);
        product.setPrice(new BigDecimal("100.00"));
        product.setQuantity(10);
        product.setCategory(category);
        return product;
    }
}
