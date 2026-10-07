package com.example.crudapi.repository;

import com.example.crudapi.entity.Product;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Kiem tra tung dieu kien loc sinh ra predicate gi.
 *
 * Phai mock Criteria API vi Specification chi duoc dich sang SQL khi co EntityManager
 * that. SQL thuc te duoc doi chieu bang tay voi profile dev (xem README).
 */
@SuppressWarnings("unchecked")
class ProductSpecificationsTest {

    @Test
    @DisplayName("keyword rong -> khong sinh dieu kien nao (unrestricted)")
    void keywordMatches_shouldBeUnrestricted_whenBlank() {
        assertThat(ProductSpecifications.keywordMatches("   ").toPredicate(null, null, null)).isNull();
        assertThat(ProductSpecifications.keywordMatches(null).toPredicate(null, null, null)).isNull();
    }

    @Test
    @DisplayName("categoryId null -> khong sinh dieu kien nao (unrestricted)")
    void hasCategory_shouldBeUnrestricted_whenNull() {
        assertThat(ProductSpecifications.hasCategory(null).toPredicate(null, null, null)).isNull();
    }

    @Test
    @DisplayName("keyword duoc trim va ha ve chu thuong truoc khi dua vao LIKE")
    void keywordMatches_shouldTrimAndLowercase() {
        Root<Product> root = mock(Root.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path<String> namePath = mock(Path.class);
        Path<String> skuPath = mock(Path.class);
        Expression<String> lowerName = mock(Expression.class);
        Expression<String> lowerSku = mock(Expression.class);

        doReturn(namePath).when(root).get("name");
        doReturn(skuPath).when(root).get("sku");
        doReturn(lowerName).when(cb).lower(namePath);
        doReturn(lowerSku).when(cb).lower(skuPath);

        ProductSpecifications.keywordMatches("  ChUoT  ").toPredicate(root, null, cb);

        verify(cb).like(lowerName, "%chuot%");
        verify(cb).like(lowerSku, "%chuot%");
    }

    @Test
    @DisplayName("loc category so sanh thang voi khoa ngoai, khong join bang categories")
    void hasCategory_shouldCompareOnForeignKey() {
        Root<Product> root = mock(Root.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path<Object> categoryPath = mock(Path.class);
        Path<Object> idPath = mock(Path.class);

        doReturn(categoryPath).when(root).get("category");
        doReturn(idPath).when(categoryPath).get("id");

        ProductSpecifications.hasCategory(2L).toPredicate(root, null, cb);

        verify(cb).equal(idPath, 2L);
        verify(root, never()).join("category");
    }
}
