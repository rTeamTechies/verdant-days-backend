# Verdant Days – Database Connection (DBVisualizer / Any SQL Client)

## Database config (from `src/main/resources/application-dev.properties`)

| Setting | Value |
|---------|-------|
| **Host** | localhost |
| **Port** | 3306 |
| **Database** | verdantdays |
| **Username** | root |
| **Password** | root@123 |
| **Driver** | MariaDB (or MySQL – compatible) |

## DBVisualizer setup

1. **Create new connection** – right‑click Database → Create Database Connection → MariaDB (or MySQL)
2. **Set fields:**
   - **Database Server:** localhost  
   - **Database Port:** 3306  
   - **Database:** verdantdays  
   - **Database Userid:** root  
   - **Database Password:** root@123  
3. **Test connection** – use “Connect” or “Test”

## JDBC URL (alternative)
```
jdbc:mariadb://localhost:3306/verdantdays?useSSL=false&allowPublicKeyRetrieval=true
```
For MySQL:
```
jdbc:mysql://localhost:3306/verdantdays?useSSL=false&allowPublicKeyRetrieval=true
```

## Create database (if it doesn't exist)
```sql
CREATE DATABASE IF NOT EXISTS verdantdays 
  CHARACTER SET utf8mb4 
  COLLATE utf8mb4_unicode_ci;
```

## Change DB config
Edit `src/main/resources/application-dev.properties` and set:
- `spring.datasource.url` – JDBC URL  
- `spring.datasource.username`  
- `spring.datasource.password`  
