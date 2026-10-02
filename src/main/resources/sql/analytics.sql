-- QuickBite MySQL 8 analytics examples.
-- Parameters are shown as named placeholders (for example :from_date) for clarity.

-- 1. Top restaurants by average rating (ORDER BY, LIMIT).
SELECT r.id, r.name, r.cuisine, r.rating
FROM restaurants r
WHERE r.active = TRUE
ORDER BY r.rating DESC, r.name
LIMIT 10;

-- 2. Top-selling menu items (INNER JOIN, SUM, GROUP BY).
SELECT mi.id, mi.name, r.name AS restaurant_name, SUM(oi.quantity) AS units_sold
FROM order_items oi
INNER JOIN menu_items mi ON mi.id = oi.menu_item_id
INNER JOIN orders o ON o.id = oi.order_id
INNER JOIN restaurants r ON r.id = o.restaurant_id
WHERE o.status <> 'CANCELLED'
GROUP BY mi.id, mi.name, r.name
ORDER BY units_sold DESC
LIMIT 20;

-- 3. Revenue by restaurant (SUM, COUNT, GROUP BY).
SELECT r.id, r.name, COUNT(o.id) AS order_count, SUM(o.total_amount) AS revenue
FROM restaurants r
INNER JOIN orders o ON o.restaurant_id = r.id
WHERE o.status <> 'CANCELLED'
GROUP BY r.id, r.name
ORDER BY revenue DESC;

-- 4. Revenue by month (DATE_FORMAT, date filtering).
SELECT DATE_FORMAT(o.placed_at, '%Y-%m') AS order_month,
       COUNT(*) AS order_count,
       SUM(o.total_amount) AS revenue
FROM orders o
WHERE o.status <> 'CANCELLED'
  AND o.placed_at >= :from_date
  AND o.placed_at < :to_date
GROUP BY DATE_FORMAT(o.placed_at, '%Y-%m')
ORDER BY order_month;

-- 5. Orders per customer (LEFT JOIN includes customers with no orders).
SELECT u.id, u.email, COUNT(o.id) AS order_count
FROM users u
LEFT JOIN orders o ON o.user_id = u.id
GROUP BY u.id, u.email
ORDER BY order_count DESC, u.id;

-- 6. Customers with more than the supplied order threshold (HAVING).
SELECT u.id, u.email, COUNT(o.id) AS order_count
FROM users u
INNER JOIN orders o ON o.user_id = u.id
WHERE o.status <> 'CANCELLED'
GROUP BY u.id, u.email
HAVING COUNT(o.id) > :minimum_order_count
ORDER BY order_count DESC;

-- 7. Restaurants with no orders (LEFT JOIN, NULL filtering).
SELECT r.id, r.name, r.city
FROM restaurants r
LEFT JOIN orders o ON o.restaurant_id = r.id
WHERE o.id IS NULL
ORDER BY r.name;

-- 8. Most popular cuisine by completed order count (multi-table aggregation).
SELECT r.cuisine, COUNT(o.id) AS delivered_orders
FROM restaurants r
INNER JOIN orders o ON o.restaurant_id = r.id
WHERE o.status = 'DELIVERED'
GROUP BY r.cuisine
ORDER BY delivered_orders DESC
LIMIT 1;

-- 9. Average order value by restaurant (AVG, NULL-safe).
SELECT r.id, r.name, AVG(o.total_amount) AS average_order_value
FROM restaurants r
LEFT JOIN orders o ON o.restaurant_id = r.id AND o.status <> 'CANCELLED'
GROUP BY r.id, r.name
ORDER BY average_order_value DESC;

-- 10. Daily order counts and gross value for a date range.
SELECT DATE(o.placed_at) AS order_date,
       COUNT(*) AS order_count,
       SUM(o.total_amount) AS gross_order_value
FROM orders o
WHERE o.placed_at >= :from_date
  AND o.placed_at < :to_date
GROUP BY DATE(o.placed_at)
ORDER BY order_date;

-- 11. Cancellation percentage for a date range.
SELECT COUNT(*) AS total_orders,
       SUM(o.status = 'CANCELLED') AS cancelled_orders,
       ROUND(100.0 * SUM(o.status = 'CANCELLED') / NULLIF(COUNT(*), 0), 2) AS cancelled_percentage
FROM orders o
WHERE o.placed_at >= :from_date
  AND o.placed_at < :to_date;

-- 12. Highest-spending customers (non-cancelled order value).
SELECT u.id, u.email, SUM(o.total_amount) AS lifetime_spend
FROM users u
INNER JOIN orders o ON o.user_id = u.id
WHERE o.status <> 'CANCELLED'
GROUP BY u.id, u.email
ORDER BY lifetime_spend DESC
LIMIT 20;

-- 13. Restaurants with revenue above the average restaurant revenue (subquery).
SELECT restaurant_revenue.restaurant_id,
       restaurant_revenue.restaurant_name,
       restaurant_revenue.revenue
FROM (
    SELECT r.id AS restaurant_id,
           r.name AS restaurant_name,
           COALESCE(SUM(o.total_amount), 0) AS revenue
    FROM restaurants r
    LEFT JOIN orders o ON o.restaurant_id = r.id AND o.status <> 'CANCELLED'
    GROUP BY r.id, r.name
) AS restaurant_revenue
WHERE restaurant_revenue.revenue > (
    SELECT AVG(per_restaurant.revenue)
    FROM (
        SELECT r2.id, COALESCE(SUM(o2.total_amount), 0) AS revenue
        FROM restaurants r2
        LEFT JOIN orders o2 ON o2.restaurant_id = r2.id AND o2.status <> 'CANCELLED'
        GROUP BY r2.id
    ) AS per_restaurant
)
ORDER BY restaurant_revenue.revenue DESC;

-- 14. Menu items that have never appeared in an order (NOT EXISTS correlated subquery).
SELECT mi.id, mi.restaurant_id, mi.name, mi.category, mi.price
FROM menu_items mi
WHERE NOT EXISTS (
    SELECT 1
    FROM order_items oi
    WHERE oi.menu_item_id = mi.id
)
ORDER BY mi.restaurant_id, mi.name;

-- 15. Monthly restaurant performance without multiplying order totals by line count.
WITH monthly_orders AS (
    SELECT o.restaurant_id,
           DATE_FORMAT(o.placed_at, '%Y-%m') AS order_month,
           COUNT(*) AS order_count,
           SUM(o.total_amount) AS revenue,
           AVG(o.total_amount) AS average_order_value
    FROM orders o
    WHERE o.status <> 'CANCELLED'
      AND o.placed_at >= :from_date
      AND o.placed_at < :to_date
    GROUP BY o.restaurant_id, DATE_FORMAT(o.placed_at, '%Y-%m')
),
monthly_units AS (
    SELECT o.restaurant_id,
           DATE_FORMAT(o.placed_at, '%Y-%m') AS order_month,
           SUM(oi.quantity) AS units_sold
    FROM orders o
    INNER JOIN order_items oi ON oi.order_id = o.id
    WHERE o.status <> 'CANCELLED'
      AND o.placed_at >= :from_date
      AND o.placed_at < :to_date
    GROUP BY o.restaurant_id, DATE_FORMAT(o.placed_at, '%Y-%m')
)
SELECT r.id, r.name, monthly_orders.order_month, monthly_orders.order_count,
       monthly_units.units_sold, monthly_orders.revenue, monthly_orders.average_order_value
FROM monthly_orders
INNER JOIN monthly_units
    ON monthly_units.restaurant_id = monthly_orders.restaurant_id
   AND monthly_units.order_month = monthly_orders.order_month
INNER JOIN restaurants r ON r.id = monthly_orders.restaurant_id
ORDER BY monthly_orders.order_month, monthly_orders.revenue DESC;

-- 16. Price range by menu category (MIN/MAX and COUNT).
SELECT r.name AS restaurant_name,
       mi.category,
       COUNT(*) AS item_count,
       MIN(mi.price) AS lowest_price,
       MAX(mi.price) AS highest_price,
       AVG(mi.price) AS average_price
FROM menu_items mi
INNER JOIN restaurants r ON r.id = mi.restaurant_id
WHERE mi.available = TRUE
GROUP BY r.id, r.name, mi.category
ORDER BY r.name, mi.category;

-- 17. Customer order history with delivery address details (multi-table join).
SELECT u.email,
       o.id AS order_id,
       o.status,
       o.total_amount,
       o.placed_at,
       a.city,
       a.postal_code
FROM orders o
INNER JOIN users u ON u.id = o.user_id
INNER JOIN addresses a ON a.id = o.address_id
WHERE o.user_id = :user_id
ORDER BY o.placed_at DESC;

-- 18. Restaurants whose trailing 30-day revenue exceeds the per-restaurant average.
WITH trailing_revenue AS (
    SELECT r.id AS restaurant_id, r.name AS restaurant_name,
           COALESCE(SUM(o.total_amount), 0) AS revenue
    FROM restaurants r
    LEFT JOIN orders o
        ON o.restaurant_id = r.id
       AND o.status <> 'CANCELLED'
       AND o.placed_at >= CURRENT_TIMESTAMP - INTERVAL 30 DAY
    GROUP BY r.id, r.name
)
SELECT restaurant_id, restaurant_name, revenue
FROM trailing_revenue
WHERE revenue > (SELECT AVG(revenue) FROM trailing_revenue)
ORDER BY revenue DESC;
