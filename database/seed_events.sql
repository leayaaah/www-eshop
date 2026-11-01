-- Demo tracking data only. It does not change stock or create orders.
-- Replace user/product IDs when the database uses different IDs.
INSERT INTO user_event (session_id, username, user_id, event_type, product_id, value_ms, created_at_v2)
SELECT 'seed-customer1', 'customer1', u.id, 'VIEW', p.id, NULL, CURRENT_TIMESTAMP
FROM users u JOIN product p ON p.name LIKE '%iPhone%' OR p.name LIKE '%Laptop%'
WHERE u.username = 'customer1'
LIMIT 4;

INSERT INTO user_event (session_id, username, user_id, event_type, product_id, value_ms, created_at_v2)
SELECT 'seed-customer2', 'customer2', u.id, 'VIEW', p.id, NULL, CURRENT_TIMESTAMP
FROM users u JOIN product p ON p.name LIKE '%Chuột%' OR p.name LIKE '%Bàn phím%'
WHERE u.username = 'customer2'
LIMIT 4;
