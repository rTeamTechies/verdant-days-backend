package com.verdantdays.config;

import java.sql.Connection;
import java.sql.Statement;
import javax.sql.DataSource;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * Runs before Hibernate ddl-auto so enum/value changes do not truncate data.
 */
@Configuration
public class PaymentStatusMigration {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public static BeanFactoryPostProcessor paymentStatusMigratorDependsOn() {
        return beanFactory -> {
            if (!beanFactory.containsBeanDefinition("entityManagerFactory")) {
                return;
            }
            BeanDefinition emf = beanFactory.getBeanDefinition("entityManagerFactory");
            String[] existing = emf.getDependsOn();
            if (existing == null || existing.length == 0) {
                emf.setDependsOn("orderSchemaMigrator");
            } else {
                String[] next = new String[existing.length + 1];
                System.arraycopy(existing, 0, next, 0, existing.length);
                next[existing.length] = "orderSchemaMigrator";
                emf.setDependsOn(next);
            }
        };
    }

    @Bean(name = "orderSchemaMigrator")
    public Object orderSchemaMigrator(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            // Widen enum-ish columns to VARCHAR first
            safe(stmt, "ALTER TABLE orders MODIFY COLUMN order_type VARCHAR(30) NOT NULL");
            safe(stmt, "ALTER TABLE orders MODIFY COLUMN order_status VARCHAR(30) NULL");
            safe(stmt, "ALTER TABLE orders MODIFY COLUMN payment_status VARCHAR(20) NOT NULL");
            safe(stmt, "ALTER TABLE orders MODIFY COLUMN payment_type VARCHAR(20) NOT NULL");
            safe(stmt, "ALTER TABLE orders MODIFY COLUMN customer_id INT NULL");

            // Discount / subtotal columns
            safe(stmt, "ALTER TABLE orders ADD COLUMN subtotal_amount DECIMAL(10,2) NOT NULL DEFAULT 0");
            safe(stmt, "ALTER TABLE orders ADD COLUMN discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0");
            safe(stmt, "UPDATE orders SET subtotal_amount = total_amount WHERE subtotal_amount = 0 OR subtotal_amount IS NULL");

            // Product description + nullable oil/size
            safe(stmt, "ALTER TABLE products ADD COLUMN description VARCHAR(500) NULL");
            safe(stmt, "ALTER TABLE products MODIFY COLUMN oil_type VARCHAR(20) NULL");
            safe(stmt, "ALTER TABLE products MODIFY COLUMN size VARCHAR(10) NULL");

            // Message templates on bill_settings
            safe(stmt, "ALTER TABLE bill_settings ADD COLUMN order_whatsapp_template TEXT NULL");
            safe(stmt, "ALTER TABLE bill_settings ADD COLUMN reminder_subscribed_template TEXT NULL");
            safe(stmt, "ALTER TABLE bill_settings ADD COLUMN reminder_non_subscribed_template TEXT NULL");

            // Walk-in customers may not have a phone number
            safe(stmt, "ALTER TABLE customers MODIFY COLUMN phone_number VARCHAR(15) NULL");

            // Order type migration
            safe(stmt, "UPDATE orders SET order_type = 'RETAIL' WHERE order_type IN ('DIRECT_SALE','RETAIL')");
            safe(stmt, "UPDATE orders SET order_type = 'SCHEDULED_DELIVERY' WHERE order_type IN ('DELIVERY','SCHEDULED_DELIVERY')");
            safe(stmt, "UPDATE orders SET order_type = 'WEBSITE' WHERE order_source = 'WEBSITE' OR order_type = 'WEBSITE'");

            // Order status migration
            safe(stmt, "UPDATE orders SET order_status = 'PLACED' WHERE order_status IN ('ORDER_PLACED','PLACED') OR order_status IS NULL OR order_status = ''");
            safe(stmt, "UPDATE orders SET order_status = 'PACKED' WHERE order_status = 'PACKED'");
            safe(stmt, "UPDATE orders SET order_status = 'IN_TRANSIT' WHERE order_status = 'IN_TRANSIT'");
            safe(stmt, "UPDATE orders SET order_status = 'DELIVERED' WHERE order_status IN ('DELIVERED','COMPLETED','PAID','DUE_OR_PAID')");

            // Payment status migration
            safe(stmt, "UPDATE orders SET payment_status = 'PAID' WHERE payment_status IN ('RECEIVED','PAID') OR payment_status IS NULL OR payment_status = ''");
            safe(stmt, "UPDATE orders SET payment_status = 'PARTIAL_PAID' WHERE payment_status IN ('PARTIAL','PARTIAL_PAID')");
            safe(stmt, "UPDATE orders SET payment_status = 'DUE' WHERE payment_status IN ('PENDING','DUE')");
            safe(stmt, "UPDATE orders SET payment_status = 'PAID' WHERE payment_status NOT IN ('PAID','PARTIAL_PAID','DUE')");
        } catch (Exception ignored) {
            // First boot / H2 tests may not have tables yet
        }
        return new Object();
    }

    private static void safe(Statement stmt, String sql) {
        try {
            stmt.execute(sql);
        } catch (Exception ignored) {
            // column already exists / wrong dialect — ignore
        }
    }
}
