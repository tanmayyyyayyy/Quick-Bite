# Data Model and SQL Notes

## Normalization

Core facts are stored once: users, restaurants, menu items, addresses, carts, and orders are separate relations. `cart_items` associates a cart with a menu item and enforces one row per cart/menu-item pair. `order_items` records purchased quantity and a unit-price/name snapshot, preserving order history when menu prices or names later change. User and restaurant address fields are not duplicated into order rows; orders reference the selected address.

## Constraints and Indexes

The Flyway V1 migration defines primary and foreign keys, unique account email/phone, unique restaurant/menu-item name, unique cart per user, unique menu item per cart, numeric check constraints, and role/status checks. Indexes support user role lookup, restaurant cuisine/rating and city/activity filters, menu availability/category, cart ownership, and user/restaurant/status/time order queries. Foreign keys deliberately restrict deleting referenced business history; cart/order item rows cascade with their parent aggregate.

## Relationships and Query Shape

Many-to-one references are lazy. Collections are lazy and not serialized by controllers. API responses are explicit DTOs. Cart reads use an entity graph for cart items, menu item, and restaurant. Detailed order reads use an entity graph for order items, menu items, user, restaurant, and address. Pageable repository queries handle catalog and order filtering.

## Transactions and ACID

Order creation runs in one transaction: it validates the customer-owned address and cart, checks current availability/quantities/single-restaurant rules, computes monetary totals with `BigDecimal`, snapshots order lines, persists the order, and clears the cart. Any unchecked failure rolls back those writes. MySQL InnoDB provides atomicity, consistency through constraints, isolation according to the transaction isolation level, and durability after commit. Production deployments should configure backups and monitor transaction duration/locking.

## EXPLAIN

Use `EXPLAIN` on the analytics and repository-equivalent SQL to inspect join order and index selection. For example:

```sql
EXPLAIN
SELECT r.id, r.name, SUM(o.total_amount) AS revenue
FROM restaurants r
JOIN orders o ON o.restaurant_id = r.id
WHERE o.status <> 'CANCELLED'
  AND o.created_at >= '2026-01-01'
GROUP BY r.id, r.name
ORDER BY revenue DESC;
```

Check that the plan uses the restaurant/status/time order indexes where appropriate, examine estimated rows and temporary/filesort operations, and validate with representative data before changing indexes. Query behavior can vary with actual table cardinality and the selected date range.

## Analytics Examples

See [`src/main/resources/sql/analytics.sql`](../src/main/resources/sql/analytics.sql) for 18 documented MySQL queries covering joins, date filters, grouped counts, revenue, cancellation rates, subqueries, correlated `NOT EXISTS`, and monthly performance. Named parameters are illustrative and should be bound by the caller.
