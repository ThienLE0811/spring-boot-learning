INSERT INTO categories (name, description, version, created_at, updated_at)
VALUES
    ('Phu kien may tinh', 'Ban phim, chuot, tai nghe va phu kien khac', 0, NOW(), NOW()),
    ('Man hinh', 'Man hinh may tinh cac loai', 0, NOW(), NOW());

UPDATE products
SET category_id = (SELECT id FROM categories WHERE name = 'Phu kien may tinh')
WHERE sku IN ('SKU-001', 'SKU-002');

UPDATE products
SET category_id = (SELECT id FROM categories WHERE name = 'Man hinh')
WHERE sku = 'SKU-003';
