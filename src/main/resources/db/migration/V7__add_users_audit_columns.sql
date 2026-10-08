-- Dong bo bang users voi categories/products: them optimistic locking va dau thoi gian sua.
--
-- Bang users DA CO ban ghi 'admin' tu V6, nen cot NOT NULL bat buoc phai di kem DEFAULT.
-- Khong co DEFAULT thi PostgreSQL khong biet dien gi vao dong cu va migration fail ngay.
-- Giu lai DEFAULT sau khi them cung khong hai: Hibernate luon tu gui gia tri cho 2 cot nay,
-- DEFAULT chi con la luoi do cho cac lenh INSERT thu cong (vi du file seed cua Flyway).

ALTER TABLE users
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE users
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT now();
