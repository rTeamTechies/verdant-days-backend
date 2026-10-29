package com.verdantdays.config;

import com.verdantdays.model.*;
import com.verdantdays.repository.AdminUserRepository;
import com.verdantdays.repository.ApartmentRepository;
import com.verdantdays.repository.DeliverySlotRepository;
import com.verdantdays.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Bootstraps admin login and default oil catalog only.
 * Does not seed dummy customers, orders, or apartments.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ApartmentRepository apartmentRepository;

    @Autowired
    private DeliverySlotRepository deliverySlotRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedAdminUser();
        seedProducts();
        backfillWebsiteFields();
    }

    private void seedAdminUser() {
        AdminUser admin = adminUserRepository.findByPhoneNumber("8015761437")
                .orElse(new AdminUser());
        admin.setPhoneNumber("8015761437");
        admin.setPasswordHash(passwordEncoder.encode("Admin@123"));
        admin.setName("Admin");
        admin.setActive(true);
        adminUserRepository.save(admin);
    }

    private void seedProducts() {
        List<Product> defaults = List.of(
                createProduct(Product.OilType.GROUNDNUT, Product.Size.ML_500, new BigDecimal("130"), new BigDecimal("175"), "Groundnut Oil 500ml"),
                createProduct(Product.OilType.GROUNDNUT, Product.Size.L_1, new BigDecimal("250"), new BigDecimal("325"), "Groundnut Oil 1L"),
                createProduct(Product.OilType.SESAME, Product.Size.ML_500, new BigDecimal("180"), new BigDecimal("240"), "Sesame (Gingely) Oil 500ml"),
                createProduct(Product.OilType.SESAME, Product.Size.L_1, new BigDecimal("350"), new BigDecimal("450"), "Sesame (Gingely) Oil 1L"),
                createProduct(Product.OilType.COCONUT, Product.Size.ML_500, new BigDecimal("200"), new BigDecimal("270"), "Coconut Oil 500ml"),
                createProduct(Product.OilType.COCONUT, Product.Size.L_1, new BigDecimal("390"), new BigDecimal("510"), "Coconut Oil 1L")
        );
        for (Product p : defaults) {
            if (productRepository.findByOilTypeAndSizeAndActiveTrue(p.getOilType(), p.getSize()).isEmpty()) {
                productRepository.save(p);
            }
        }
    }

    private Product createProduct(Product.OilType type, Product.Size size, BigDecimal price, BigDecimal mrp, String displayName) {
        Product p = new Product();
        p.setOilType(type);
        p.setSize(size);
        p.setPrice(price);
        p.setMrp(mrp);
        p.setDisplayName(displayName);
        p.setActive(true);
        return p;
    }

    private void backfillWebsiteFields() {
        for (Product p : productRepository.findAll()) {
            if (p.getMrp() == null) {
                p.setMrp(defaultMrp(p.getOilType(), p.getSize(), p.getPrice()));
                productRepository.save(p);
            }
        }
        for (Apartment apt : apartmentRepository.findAll()) {
            if (apt.getPromoCode() == null || apt.getPromoCode().isBlank()) {
                apt.setPromoCode(promoFromName(apt.getName(), apt.getId()));
                apartmentRepository.save(apt);
            }
            seedUpcomingSlots(apt);
        }
    }

    private void seedUpcomingSlots(Apartment apt) {
        if (!deliverySlotRepository.findByApartmentIdAndActiveTrueOrderByDeliveryDateAsc(apt.getId()).isEmpty()) {
            return;
        }
        for (LocalDate date : List.of(nextDayOfMonth(10), nextDayOfMonth(25), nextDayOfMonth(10).plusMonths(1), nextDayOfMonth(25).plusMonths(1))) {
            if (deliverySlotRepository.existsByApartmentIdAndDeliveryDateAndActiveTrue(apt.getId(), date)) continue;
            DeliverySlot slot = new DeliverySlot();
            slot.setApartment(apt);
            slot.setDeliveryDate(date);
            slot.setActive(true);
            deliverySlotRepository.save(slot);
        }
    }

    private LocalDate nextDayOfMonth(int day) {
        LocalDate today = LocalDate.now();
        int safeDay = Math.min(day, today.lengthOfMonth());
        LocalDate candidate = today.withDayOfMonth(safeDay);
        if (!candidate.isAfter(today)) {
            LocalDate next = today.plusMonths(1);
            candidate = next.withDayOfMonth(Math.min(day, next.lengthOfMonth()));
        }
        return candidate;
    }

    private String promoFromName(String name, Integer id) {
        String first = name == null ? "APT" : name.split(",")[0].trim().split("\\s+")[0].toUpperCase().replaceAll("[^A-Z]", "");
        if (first.isEmpty()) first = "APT";
        String code = first;
        int n = 0;
        while (apartmentRepository.findByPromoCodeIgnoreCaseAndActiveTrue(code).filter(a -> !a.getId().equals(id)).isPresent()) {
            n++;
            code = first + n;
        }
        return code;
    }

    private BigDecimal defaultMrp(Product.OilType type, Product.Size size, BigDecimal price) {
        if (type == Product.OilType.GROUNDNUT && size == Product.Size.ML_500) return new BigDecimal("175");
        if (type == Product.OilType.GROUNDNUT && size == Product.Size.L_1) return new BigDecimal("325");
        if (type == Product.OilType.SESAME && size == Product.Size.ML_500) return new BigDecimal("240");
        if (type == Product.OilType.SESAME && size == Product.Size.L_1) return new BigDecimal("450");
        if (type == Product.OilType.COCONUT && size == Product.Size.ML_500) return new BigDecimal("270");
        if (type == Product.OilType.COCONUT && size == Product.Size.L_1) return new BigDecimal("510");
        return price;
    }
}
