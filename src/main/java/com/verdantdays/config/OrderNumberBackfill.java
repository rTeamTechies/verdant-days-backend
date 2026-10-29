package com.verdantdays.config;

import com.verdantdays.model.Order;
import com.verdantdays.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class OrderNumberBackfill implements ApplicationRunner {

    @Autowired
    private OrderRepository orderRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Order> withoutNumber = orderRepository.findAll().stream()
                .filter(o -> o.getOrderNumber() == null || o.getOrderNumber().isEmpty())
                .sorted(Comparator.comparing(Order::getCreatedAt).thenComparing(Order::getId))
                .toList();
        if (withoutNumber.isEmpty()) return;
        // Sequence resets per calendar month; date part stays YYMMDD of the order day.
        Map<String, List<Order>> byMonth = withoutNumber.stream()
                .collect(Collectors.groupingBy(o -> o.getCreatedAt().format(DateTimeFormatter.ofPattern("yyMM"))));
        for (List<Order> group : byMonth.values()) {
            group.sort(Comparator.comparing(Order::getCreatedAt).thenComparing(Order::getId));
            int seq = 1;
            for (Order o : group) {
                String datePart = o.getCreatedAt().format(DateTimeFormatter.ofPattern("yyMMdd"));
                o.setOrderNumber(datePart + "-" + String.format("%02d", seq++));
            }
        }
        orderRepository.saveAll(withoutNumber);

        List<Order> withoutTypeOrStatus = orderRepository.findAll().stream()
                .filter(o -> o.getOrderType() == null || o.getOrderStatus() == null)
                .toList();
        if (!withoutTypeOrStatus.isEmpty()) {
            for (Order o : withoutTypeOrStatus) {
                if (o.getOrderType() == null) o.setOrderType(Order.OrderType.RETAIL);
                if (o.getOrderStatus() == null) o.setOrderStatus(Order.OrderStatus.DELIVERED);
            }
            orderRepository.saveAll(withoutTypeOrStatus);
        }
    }
}
