-- Demo tracking data only. It does not change stock or create orders.
-- Replace user/product IDs when the database uses different IDs.
INSERT INTO user_event (session_id, username, event_type, product_id, value_ms, created_at_v2)
SELECT 'seed-customer1', 'customer1', 'VIEW', p.id, NULL, CURRENT_TIMESTAMP
FROM product p
WHERE p.name LIKE '%iPhone%' OR p.name LIKE '%Laptop%'
LIMIT 4;

INSERT INTO user_event (session_id, username, event_type, product_id, value_ms, created_at_v2)
SELECT 'seed-customer2', 'customer2', 'VIEW', p.id, NULL, CURRENT_TIMESTAMP
FROM product p
WHERE p.name LIKE '%Chuột%' OR p.name LIKE '%Bàn phím%'
LIMIT 4;
