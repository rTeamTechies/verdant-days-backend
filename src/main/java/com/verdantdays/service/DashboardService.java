package com.verdantdays.service;

import com.verdantdays.model.Order;
import com.verdantdays.model.OrderItem;
import com.verdantdays.model.Product;
import com.verdantdays.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    @Autowired
    private OrderRepository orderRepository;

    public Map<String, Object> getStats(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        List<Order> orders = orderRepository.findByCreatedAtBetweenWithItems(start, end);

        BigDecimal totalSales = orders.stream()
                .map(Order::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double totalLitres = 0;
        for (Order order : orders) {
            if (order.getItems() == null) continue;
            for (OrderItem item : order.getItems()) {
                totalLitres += litresForItem(item);
            }
        }

        Map<String, Double> oilWiseLitres = new LinkedHashMap<>();
        oilWiseLitres.put("Groundnut", 0.0);
        oilWiseLitres.put("Sesame", 0.0);
        oilWiseLitres.put("Coconut", 0.0);
        oilWiseLitres.put("Other / Combo", 0.0);
        for (Order order : orders) {
            if (order.getItems() == null) continue;
            for (OrderItem item : order.getItems()) {
                Product p = item.getProduct();
                if (p == null) continue;
                double litres = litresForItem(item);
                String oilKey = formatOilType(p.getOilType());
                oilWiseLitres.merge(oilKey, litres, Double::sum);
            }
        }

        Map<String, BigDecimal> productWiseSales = new LinkedHashMap<>();
        for (Order order : orders) {
            if (order.getItems() == null) continue;
            for (OrderItem item : order.getItems()) {
                Product p = item.getProduct();
                if (p == null) continue;
                String key = p.getDisplayName() != null ? p.getDisplayName() : ("Product #" + p.getId());
                BigDecimal line = item.getSubtotal() != null ? item.getSubtotal() : BigDecimal.ZERO;
                productWiseSales.merge(key, line, BigDecimal::add);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalSales", totalSales);
        result.put("orderCount", orders.size());
        result.put("totalLitresSold", totalLitres);
        result.put("oilWiseLitresSold", oilWiseLitres);
        result.put("productWiseSales", productWiseSales);
        result.put("salesByDate", orders.stream()
                .filter(o -> o.getCreatedAt() != null)
                .collect(Collectors.groupingBy(o -> o.getCreatedAt().toLocalDate().toString(),
                        Collectors.reducing(BigDecimal.ZERO,
                                o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO,
                                BigDecimal::add))));

        return result;
    }

    /**
     * Monthly sales totals for a calendar year (Jan–Dec). Independent of the date-range stats.
     */
    public Map<String, Object> getMonthlySales(int year) {
        List<Object[]> rows = orderRepository.sumSalesByMonthForYear(year);
        Map<Integer, BigDecimal> salesByMonth = new HashMap<>();
        Map<Integer, Long> ordersByMonth = new HashMap<>();
        for (Object[] row : rows) {
            int month = ((Number) row[0]).intValue();
            BigDecimal sales = row[1] instanceof BigDecimal bd ? bd : new BigDecimal(row[1].toString());
            long count = ((Number) row[2]).longValue();
            salesByMonth.put(month, sales);
            ordersByMonth.put(month, count);
        }

        String[] labels = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        List<Map<String, Object>> months = new ArrayList<>();
        BigDecimal yearTotal = BigDecimal.ZERO;
        long yearOrders = 0;
        for (int m = 1; m <= 12; m++) {
            BigDecimal sales = salesByMonth.getOrDefault(m, BigDecimal.ZERO);
            long count = ordersByMonth.getOrDefault(m, 0L);
            yearTotal = yearTotal.add(sales);
            yearOrders += count;
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("month", m);
            entry.put("label", labels[m - 1]);
            entry.put("totalSales", sales);
            entry.put("orderCount", count);
            months.add(entry);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("year", year);
        result.put("totalSales", yearTotal);
        result.put("orderCount", yearOrders);
        result.put("months", months);
        return result;
    }

    private static double litresForItem(OrderItem item) {
        if (item == null || item.getProduct() == null) return 0;
        Product p = item.getProduct();
        int qty = item.getQuantity() != null ? item.getQuantity() : 0;
        double litresPerUnit = (p.getSize() == Product.Size.ML_500) ? 0.5
                : (p.getSize() == Product.Size.L_1) ? 1.0
                : 0.0; // combo / unknown size — don't invent litres
        return qty * litresPerUnit;
    }

    private static String formatOilType(Product.OilType type) {
        if (type == null) return "Other / Combo";
        return switch (type) {
            case GROUNDNUT -> "Groundnut";
            case SESAME -> "Sesame";
            case COCONUT -> "Coconut";
        };
    }
}
