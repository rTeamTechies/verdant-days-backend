package com.verdantdays.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WebsiteOrderSchemaMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public WebsiteOrderSchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        widen("orders", "payment_status", "VARCHAR(20) NOT NULL");
        widen("orders", "order_status", "VARCHAR(20) NULL");
        widen("orders", "order_type", "VARCHAR(20) NOT NULL");
        widen("orders", "payment_type", "VARCHAR(20) NOT NULL");
        widen("orders", "order_source", "VARCHAR(20) NULL");
        widen("orders", "fulfillment_type", "VARCHAR(20) NULL");
        widen("orders", "promo_code", "VARCHAR(40) NULL");
        widen("orders", "shipping_address", "VARCHAR(500) NULL");
        widen("products", "mrp", "DECIMAL(10,2) NULL");
        widen("apartments", "promo_code", "VARCHAR(40) NULL");
    }

    private void widen(String table, String column, String definition) {
        try {
            jdbcTemplate.execute("ALTER TABLE " + table + " MODIFY " + column + " " + definition);
        } catch (Exception ignored) {
            // column may not exist yet; Hibernate ddl-auto can create it
        }
    }
}
