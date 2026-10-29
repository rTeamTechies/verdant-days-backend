# Verdant Days Backend

Backend API for Verdant Days – Cold-Pressed Oils Sales Management.

## Tech Stack
- Java 17, Spring Boot 3.2, Spring Security, JPA, MariaDB/MySQL

## Setup

1. **Create database**
   ```bash
   mysql -u root -p -e "CREATE DATABASE verdantdays CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
   ```

2. **Configure** `src/main/resources/application-dev.properties`:
   - Update `spring.datasource.url`, `username`, `password` if needed
   - Default: `localhost:3306/verdantdays`, user `root`, password `root@123`

3. **Run**
   ```bash
   mvn spring-boot:run
   ```
   API: http://localhost:8085

## Default Login
- **Phone:** 8015761437
- **Password:** Admin@123

## API Endpoints
- `POST /api/auth/login` – Login (phone + password)
- `GET /api/products` – List products
- `GET/POST/PUT/DELETE /api/apartments` – Apartments
- `GET/POST/PUT/DELETE /api/customers` – Customers
- `GET /api/customers/search?q=` – Search customers
- `POST /api/orders` – Create order `{customerId, items: [{productId, quantity}]}`
- `GET /api/orders/{id}/whatsapp-message` – Get WhatsApp URL for order confirmation

## Product Pricing (seeded)
- Groundnut Oil 500ml: ₹130
- Groundnut Oil 1L: ₹250
- Sesame (Gingely) Oil 500ml: ₹180
- Sesame Oil 1L: ₹350
- Coconut Oil 500ml: ₹200
- Coconut Oil 1L: ₹390
