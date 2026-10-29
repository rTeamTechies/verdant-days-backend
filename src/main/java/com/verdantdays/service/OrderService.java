package com.verdantdays.service;

import com.verdantdays.model.*;
import com.verdantdays.repository.OrderRepository;
import com.verdantdays.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StockService stockService;

    /**
     * Format: YYMMDD-NN (e.g. 261017-01). Sequence resets at the start of each month.
     * Uses max existing suffix for YYMM* (not row count) to avoid duplicates after imports/deletes.
     */
    public String generateOrderNumber() {
        return generateOrderNumber(LocalDate.now());
    }

    public String generateOrderNumber(LocalDate orderDate) {
        LocalDate date = orderDate != null ? orderDate : LocalDate.now();
        String datePart = String.format("%02d%02d%02d",
                date.getYear() % 100, date.getMonthValue(), date.getDayOfMonth());
        String ymPrefix = String.format("%02d%02d", date.getYear() % 100, date.getMonthValue());
        int nextSeq = nextMonthlySequence(ymPrefix);
        String candidate = datePart + "-" + String.format("%02d", nextSeq);
        // Extra safety if a concurrent insert raced us
        int guard = 0;
        while (orderRepository.existsByOrderNumber(candidate) && guard++ < 50) {
            nextSeq++;
            candidate = datePart + "-" + String.format("%02d", nextSeq);
        }
        return candidate;
    }

    private int nextMonthlySequence(String ymPrefix) {
        int max = 0;
        for (String num : orderRepository.findOrderNumbersByYearMonthPrefix(ymPrefix)) {
            if (num == null) continue;
            int dash = num.lastIndexOf('-');
            if (dash < 0 || dash == num.length() - 1) continue;
            // Only YYMMDD-NN style (ignore legacy IMP-...)
            String head = num.substring(0, dash);
            if (head.length() != 6 || !head.startsWith(ymPrefix)) continue;
            try {
                max = Math.max(max, Integer.parseInt(num.substring(dash + 1)));
            } catch (NumberFormatException ignored) {
            }
        }
        return max + 1;
    }

    @Transactional
    public Order createOrder(Integer customerId, List<Map<String, Object>> items, Order.PaymentType paymentType,
                             Order.OrderType orderType, Order.PaymentStatus paymentStatus, BigDecimal discountAmount,
                             BigDecimal amountPaid) {
        Order order = new Order();
        if (customerId != null) {
            order.setCustomer(customerService.getById(customerId));
        }
        order.setOrderNumber(generateOrderNumber());
        Order.OrderType type = orderType != null ? orderType : Order.OrderType.RETAIL;
        order.setOrderType(type);
        order.setOrderStatus(defaultStatusForType(type));
        order.setPaymentType(paymentType != null ? paymentType : Order.PaymentType.CASH);

        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (Map<String, Object> item : items) {
            Integer productId = toInt(item.get("productId"));
            if (productId == null) throw new RuntimeException("productId is required in each item");
            Integer qty = item.get("quantity") != null ? ((Number) item.get("quantity")).intValue() : 1;

            Product product = productRepository.findById(productId).orElseThrow(() -> new RuntimeException("Product not found: " + productId));
            BigDecimal unitPrice = product.getPrice();
            BigDecimal line = unitPrice.multiply(BigDecimal.valueOf(qty));
            subtotal = subtotal.add(line);

            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setProduct(product);
            oi.setQuantity(qty);
            oi.setUnitPrice(unitPrice);
            oi.setSubtotal(line);
            orderItems.add(oi);
        }

        BigDecimal discount = normalizeDiscount(discountAmount, subtotal);
        BigDecimal total = subtotal.subtract(discount);
        Order.PaymentStatus ps = paymentStatus != null ? paymentStatus : Order.PaymentStatus.PAID;

        order.setItems(orderItems);
        order.setSubtotalAmount(subtotal);
        order.setDiscountAmount(discount);
        order.setTotalAmount(total);
        order.setPaymentStatus(ps);
        order.setAmountPaid(resolveAmountPaid(ps, total, amountPaid));

        Order saved = orderRepository.save(order);
        stockService.deductForOrder(saved);
        return saved;
    }

    @Transactional
    public Order updateOrder(Integer id, Map<String, Object> updates) {
        Order order = orderRepository.findByIdWithItems(id).orElseThrow(() -> new RuntimeException("Order not found"));
        if (updates != null && updates.containsKey("orderType")) {
            Order.OrderType ot = parseOrderTypeObj(updates.get("orderType"));
            if (ot != null) order.setOrderType(ot);
        }
        if (updates != null && updates.containsKey("orderStatus")) {
            Order.OrderStatus os = parseOrderStatusObj(updates.get("orderStatus"));
            if (os != null) order.setOrderStatus(os);
        }
        if (updates != null && updates.containsKey("paymentType")) {
            Order.PaymentType pt = parsePaymentType(updates.get("paymentType"));
            if (pt != null) order.setPaymentType(pt);
        }
        if (updates != null && updates.containsKey("paymentStatus")) {
            Order.PaymentStatus ps = parsePaymentStatusObj(updates.get("paymentStatus"));
            if (ps != null) {
                order.setPaymentStatus(ps);
                if (ps == Order.PaymentStatus.PAID) order.setAmountPaid(order.getTotalAmount());
                else if (ps == Order.PaymentStatus.DUE) order.setAmountPaid(BigDecimal.ZERO);
            }
        }
        if (updates != null && updates.containsKey("discountAmount")) {
            BigDecimal discount = toDecimal(updates.get("discountAmount"));
            BigDecimal subtotal = order.getSubtotalAmount() != null ? order.getSubtotalAmount() : order.getTotalAmount();
            discount = normalizeDiscount(discount, subtotal);
            order.setDiscountAmount(discount);
            order.setTotalAmount(subtotal.subtract(discount));
            if (order.getPaymentStatus() == Order.PaymentStatus.PAID) {
                order.setAmountPaid(order.getTotalAmount());
            }
        }
        if (updates != null && updates.containsKey("amountPaid")) {
            BigDecimal paid = toDecimal(updates.get("amountPaid"));
            if (paid != null) {
                if (paid.compareTo(BigDecimal.ZERO) < 0) paid = BigDecimal.ZERO;
                if (paid.compareTo(order.getTotalAmount()) > 0) paid = order.getTotalAmount();
                order.setAmountPaid(paid);
                if (paid.compareTo(BigDecimal.ZERO) == 0) order.setPaymentStatus(Order.PaymentStatus.DUE);
                else if (paid.compareTo(order.getTotalAmount()) >= 0) order.setPaymentStatus(Order.PaymentStatus.PAID);
                else order.setPaymentStatus(Order.PaymentStatus.PARTIAL_PAID);
            }
        }
        if (updates != null && updates.containsKey("items")) {
            Object itemsObj = updates.get("items");
            if (itemsObj instanceof List<?> list && !list.isEmpty()) {
                stockService.reverseOrderDeduction(order);
                order.getItems().clear();
                BigDecimal subtotal = BigDecimal.ZERO;
                for (Object el : list) {
                    if (!(el instanceof Map)) continue;
                    @SuppressWarnings("unchecked")
                    Map<String, Object> item = (Map<String, Object>) el;
                    Integer productId = toInt(item.get("productId"));
                    if (productId == null) continue;
                    int qty = 1;
                    Object qtyVal = item.get("quantity");
                    if (qtyVal instanceof Number n) qty = n.intValue();
                    else if (qtyVal != null) qty = Integer.parseInt(String.valueOf(qtyVal));
                    if (qty < 1) continue;
                    Product product = productRepository.findById(productId).orElseThrow(() -> new RuntimeException("Product not found: " + productId));
                    BigDecimal unitPrice = product.getPrice();
                    BigDecimal line = unitPrice.multiply(BigDecimal.valueOf(qty));
                    subtotal = subtotal.add(line);
                    OrderItem oi = new OrderItem();
                    oi.setOrder(order);
                    oi.setProduct(product);
                    oi.setQuantity(qty);
                    oi.setUnitPrice(unitPrice);
                    oi.setSubtotal(line);
                    order.getItems().add(oi);
                }
                if (order.getItems().isEmpty()) throw new RuntimeException("At least one item with quantity > 0 is required");
                BigDecimal discount = normalizeDiscount(order.getDiscountAmount(), subtotal);
                order.setSubtotalAmount(subtotal);
                order.setDiscountAmount(discount);
                order.setTotalAmount(subtotal.subtract(discount));
                if (order.getPaymentStatus() == Order.PaymentStatus.PAID) {
                    order.setAmountPaid(order.getTotalAmount());
                } else if (order.getPaymentStatus() == Order.PaymentStatus.DUE) {
                    order.setAmountPaid(BigDecimal.ZERO);
                }
                Order saved = orderRepository.save(order);
                stockService.deductForOrder(saved);
                return saved;
            }
        }
        return orderRepository.save(order);
    }

    private static Order.OrderStatus defaultStatusForType(Order.OrderType type) {
        if (type == Order.OrderType.RETAIL) return Order.OrderStatus.DELIVERED;
        return Order.OrderStatus.PLACED;
    }

    private static BigDecimal normalizeDiscount(BigDecimal discount, BigDecimal subtotal) {
        if (discount == null || discount.compareTo(BigDecimal.ZERO) < 0) return BigDecimal.ZERO;
        if (discount.compareTo(subtotal) > 0) return subtotal;
        return discount;
    }

    private static BigDecimal resolveAmountPaid(Order.PaymentStatus ps, BigDecimal total, BigDecimal amountPaid) {
        if (ps == Order.PaymentStatus.PAID) return total;
        if (ps == Order.PaymentStatus.DUE) return BigDecimal.ZERO;
        if (amountPaid != null) {
            if (amountPaid.compareTo(BigDecimal.ZERO) < 0) return BigDecimal.ZERO;
            if (amountPaid.compareTo(total) > 0) return total;
            return amountPaid;
        }
        return total.divide(BigDecimal.valueOf(2), 2, java.math.RoundingMode.HALF_UP);
    }

    private static Order.PaymentType parsePaymentType(Object o) {
        if (o == null) return Order.PaymentType.CASH;
        String s = String.valueOf(o).trim().toUpperCase();
        if ("UPI".equals(s)) return Order.PaymentType.UPI;
        return Order.PaymentType.CASH;
    }

    public Order getById(Integer id) {
        return orderRepository.findById(id).orElseThrow(() -> new RuntimeException("Order not found"));
    }

    public Order getByIdWithItems(Integer id) {
        return orderRepository.findByIdWithItems(id).orElseThrow(() -> new RuntimeException("Order not found"));
    }

    public List<Order> getByCustomer(Integer customerId) {
        return orderRepository.findByCustomerIdWithItems(customerId);
    }

    public List<Order> searchOrders(Integer customerId, Integer apartmentId, String orderType, String orderStatus, String paymentType, String paymentStatus, Integer productId, String orderNumber, LocalDate startDate, LocalDate endDate) {
        Order.PaymentType pt = parsePaymentTypeFilter(paymentType);
        Order.PaymentStatus ps = parsePaymentStatus(paymentStatus);
        Order.OrderType ot = parseOrderType(orderType);
        Order.OrderStatus os = parseOrderStatus(orderStatus);
        LocalDateTime startDt = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDt = endDate != null ? endDate.atTime(LocalTime.MAX) : null;
        return orderRepository.searchWithItems(customerId, apartmentId, ot, os, pt, ps, productId, orderNumber, startDt, endDt);
    }

    public org.springframework.data.domain.Page<Order> searchOrdersPage(Integer customerId, Integer apartmentId, String orderType, String orderStatus, String paymentType, String paymentStatus, Integer productId, String orderNumber, LocalDate startDate, LocalDate endDate, int page, int size) {
        Order.PaymentType pt = parsePaymentTypeFilter(paymentType);
        Order.PaymentStatus ps = parsePaymentStatus(paymentStatus);
        Order.OrderType ot = parseOrderType(orderType);
        Order.OrderStatus os = parseOrderStatus(orderStatus);
        LocalDateTime startDt = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDt = endDate != null ? endDate.atTime(LocalTime.MAX) : null;
        Pageable p = PageRequest.of(page, Math.min(size, 50));
        return orderRepository.searchWithItemsPage(customerId, apartmentId, ot, os, pt, ps, productId, orderNumber, startDt, endDt, p);
    }

    /** All matching orders for Excel export (capped). */
    public java.util.List<Order> listForExport(Integer customerId, Integer apartmentId, String orderType, String orderStatus,
                                               String paymentType, String paymentStatus, Integer productId, String orderNumber,
                                               LocalDate startDate, LocalDate endDate) {
        java.util.List<Order> all = new java.util.ArrayList<>();
        int page = 0;
        org.springframework.data.domain.Page<Order> result;
        do {
            result = searchOrdersPage(customerId, apartmentId, orderType, orderStatus, paymentType, paymentStatus,
                    productId, orderNumber, startDate, endDate, page, 50);
            all.addAll(result.getContent());
            for (Order o : result.getContent()) {
                if (o.getCustomer() != null) {
                    o.getCustomer().getName();
                    if (o.getCustomer().getApartment() != null) {
                        o.getCustomer().getApartment().getName();
                    }
                }
                if (o.getItems() != null) {
                    o.getItems().forEach(i -> {
                        if (i.getProduct() != null) i.getProduct().getDisplayName();
                    });
                }
            }
            page++;
        } while (page < result.getTotalPages() && all.size() < 20000);
        return all;
    }

    private static Order.PaymentType parsePaymentTypeFilter(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Order.PaymentType.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) { return null; }
    }

    private static Order.PaymentStatus parsePaymentStatus(String s) {
        if (s == null || s.isBlank()) return null;
        String n = s.trim().toUpperCase().replace(' ', '_').replace('-', '_');
        if ("PARTIAL".equals(n) || "PARTIALPAID".equals(n)) n = "PARTIAL_PAID";
        try {
            return Order.PaymentStatus.valueOf(n);
        } catch (IllegalArgumentException e) { return null; }
    }
    private static Order.PaymentStatus parsePaymentStatusObj(Object o) {
        if (o == null) return null;
        return parsePaymentStatus(String.valueOf(o));
    }

    public static Order.OrderType parseOrderType(String s) {
        if (s == null || s.isBlank()) return null;
        String n = s.trim().toUpperCase().replace(' ', '_').replace('-', '_');
        if ("DIRECT_SALE".equals(n) || "RETAIL".equals(n)) return Order.OrderType.RETAIL;
        if ("DELIVERY".equals(n) || "SCHEDULED_DELIVERY".equals(n) || "SCHEDULEDDELIVERY".equals(n)) {
            return Order.OrderType.SCHEDULED_DELIVERY;
        }
        if ("WEBSITE".equals(n)) return Order.OrderType.WEBSITE;
        try {
            return Order.OrderType.valueOf(n);
        } catch (IllegalArgumentException e) { return null; }
    }

    public static Order.OrderStatus parseOrderStatus(String s) {
        if (s == null || s.isBlank()) return null;
        String n = s.trim().toUpperCase().replace(' ', '_').replace('-', '_');
        if ("ORDER_PLACED".equals(n) || "PLACED".equals(n)) return Order.OrderStatus.PLACED;
        if ("COMPLETED".equals(n) || "PAID".equals(n) || "DUE_OR_PAID".equals(n) || "DELIVERED".equals(n)) {
            return Order.OrderStatus.DELIVERED;
        }
        if ("INTRANSIT".equals(n) || "IN_TRANSIT".equals(n)) return Order.OrderStatus.IN_TRANSIT;
        try {
            return Order.OrderStatus.valueOf(n);
        } catch (IllegalArgumentException e) { return null; }
    }
    private static Order.OrderType parseOrderTypeObj(Object o) {
        if (o == null) return null;
        return parseOrderType(String.valueOf(o));
    }
    private static Order.OrderStatus parseOrderStatusObj(Object o) {
        if (o == null) return null;
        return parseOrderStatus(String.valueOf(o));
    }

    private static Integer toInt(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.intValue();
        if (o instanceof String s && !s.isBlank()) return Integer.parseInt(s);
        return null;
    }

    private static BigDecimal toDecimal(Object o) {
        if (o == null) return null;
        if (o instanceof BigDecimal bd) return bd;
        if (o instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        try {
            return new BigDecimal(String.valueOf(o).trim());
        } catch (Exception e) {
            return null;
        }
    }
}
