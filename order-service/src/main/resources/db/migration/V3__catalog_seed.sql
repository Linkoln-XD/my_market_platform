INSERT INTO catalog.categories (id, name, slug)
VALUES ('11111111-1111-1111-1111-111111111101', 'Burgers', 'burgers'),
       ('11111111-1111-1111-1111-111111111102', 'Drinks', 'drinks'),
       ('11111111-1111-1111-1111-111111111103', 'Sides', 'sides');

INSERT INTO catalog.products (id, category_id, sku, name, description, price_cents, currency, active)
VALUES ('22222222-2222-2222-2222-222222222201', '11111111-1111-1111-1111-111111111101', 'BRG-CLASSIC',
        'Classic Burger', 'Beef patty, cheddar, house sauce', 899, 'USD', TRUE),
       ('22222222-2222-2222-2222-222222222202', '11111111-1111-1111-1111-111111111101', 'BRG-VEGGIE',
        'Veggie Burger', 'Plant patty, avocado, sprouts', 799, 'USD', TRUE),
       ('22222222-2222-2222-2222-222222222203', '11111111-1111-1111-1111-111111111102', 'DRK-COLA',
        'Cola 0.5L', 'Chilled soft drink', 249, 'USD', TRUE),
       ('22222222-2222-2222-2222-222222222204', '11111111-1111-1111-1111-111111111103', 'SID-FRIES',
        'French Fries', 'Crispy salted fries', 349, 'USD', TRUE);

INSERT INTO catalog.stock (product_id, quantity, version)
VALUES ('22222222-2222-2222-2222-222222222201', 120, 0),
       ('22222222-2222-2222-2222-222222222202', 80, 0),
       ('22222222-2222-2222-2222-222222222203', 500, 0),
       ('22222222-2222-2222-2222-222222222204', 200, 0);
