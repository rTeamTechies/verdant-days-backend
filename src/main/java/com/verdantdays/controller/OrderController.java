package com.verdantdays.controller;

import com.verdantdays.model.Order;
import com.verdantdays.model.dto.ApiResponse;
import com.verdantdays.model.dto.CreateOrderResponse;
import com.verdantdays.service.ExcelExportService;
import com.verdantdays.service.OrderService;
import com.verdantdays.service.WhatsAppMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private WhatsAppMessageService whatsAppMessageService;

    @Autowired
    private ExcelExportService excelExportService;

    @Value("${verdant.whatsapp.number:919876543210}")
    private String defaultWhatsAppNumber;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        try {
            Integer customerId = toInt(body.get("customerId"));
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
            if (items == null || items.isEmpty()) {
                return ResponseEntity.badRequest().body(new ApiResponse<>("items required", null, 400));
            }
            Order.PaymentType paymentType = parsePaymentType(body.get("paymentType"));
            Order.OrderType orderType = parseOrderType(body.get("orderType"));
            Order.PaymentStatus paymentStatus = parsePaymentStatus(body.get("paymentStatus"));
            BigDecimal discount = toDecimal(body.get("discountAmount"));
            BigDecimal amountPaid = toDecimal(body.get("amountPaid"));
            Order order = orderService.createOrder(customerId, items, paymentType, orderType, paymentStatus, discount, amountPaid);
            return ResponseEntity.ok(new ApiResponse<>("Created", new CreateOrderResponse(order.getId(), order.getOrderNumber()), 200));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(e.getMessage(), null, 400));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        try {
            Order order = orderService.updateOrder(id, body);
            return ResponseEntity.ok(new ApiResponse<>("OK", Map.of("id", order.getId(), "orderNumber", order.getOrderNumber() != null ? order.getOrderNumber() : ""), 200));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(e.getMessage(), null, 400));
        }
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(required = false) Integer customerId,
            @RequestParam(required = false) Integer apartmentId,
            @RequestParam(required = false) String orderType,
            @RequestParam(required = false) String orderStatus,
            @RequestParam(required = false) String paymentType,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) Integer productId,
            @RequestParam(required = false) String orderNumber,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate endDate) {
        var list = orderService.listForExport(customerId, apartmentId, orderType, orderStatus, paymentType, paymentStatus,
                productId, orderNumber, startDate, endDate);
        byte[] bytes = excelExportService.exportOrders(list);
        String filename = "orders-" + LocalDate.now() + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<com.verdantdays.model.dto.PageResponse<Order>>> search(
            @RequestParam(required = false) Integer customerId,
            @RequestParam(required = false) Integer apartmentId,
            @RequestParam(required = false) String orderType,
            @RequestParam(required = false) String orderStatus,
            @RequestParam(required = false) String paymentType,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) Integer productId,
            @RequestParam(required = false) String orderNumber,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        var pageResult = orderService.searchOrdersPage(customerId, apartmentId, orderType, orderStatus, paymentType, paymentStatus, productId, orderNumber, startDate, endDate, page, size);
        var pr = new com.verdantdays.model.dto.PageResponse<>(pageResult.getContent(), pageResult.getNumber(), pageResult.getSize(), pageResult.getTotalElements());
        return ResponseEntity.ok(new ApiResponse<>("OK", pr, 200));
    }

    private static Order.PaymentType parsePaymentType(Object o) {
        if (o == null) return Order.PaymentType.CASH;
        String s = o.toString().trim().toUpperCase();
        if ("UPI".equals(s)) return Order.PaymentType.UPI;
        return Order.PaymentType.CASH;
    }
    private static Order.OrderType parseOrderType(Object o) {
        if (o == null) return Order.OrderType.RETAIL;
        Order.OrderType t = OrderService.parseOrderType(String.valueOf(o));
        return t != null ? t : Order.OrderType.RETAIL;
    }
    private static Order.PaymentStatus parsePaymentStatus(Object o) {
        if (o == null) return Order.PaymentStatus.PAID;
        String s = o.toString().trim().toUpperCase().replace(' ', '_').replace('-', '_');
        if ("PARTIAL".equals(s) || "PARTIALPAID".equals(s) || "PARTIAL_PAID".equals(s)) return Order.PaymentStatus.PARTIAL_PAID;
        if ("DUE".equals(s)) return Order.PaymentStatus.DUE;
        return Order.PaymentStatus.PAID;
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

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Order>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(new ApiResponse<>("OK", orderService.getByIdWithItems(id), 200));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<ApiResponse<List<Order>>> getByCustomer(@PathVariable Integer customerId) {
        return ResponseEntity.ok(new ApiResponse<>("OK", orderService.getByCustomer(customerId), 200));
    }

    @GetMapping("/{id}/whatsapp-message")
    public ResponseEntity<ApiResponse<Map<String, String>>> getWhatsAppMessage(@PathVariable Integer id) {
        Order order = orderService.getByIdWithItems(id);
        String message = whatsAppMessageService.buildOrderConfirmationMessage(order);
        String phone = order.getCustomer() != null ? order.getCustomer().getPhoneNumber() : null;
        if (phone == null || phone.isBlank()) {
            phone = defaultWhatsAppNumber;
        }
        String url = whatsAppMessageService.getWhatsAppUrl(phone, message);
        return ResponseEntity.ok(new ApiResponse<>("OK", Map.of("message", message, "url", url, "phone", phone != null ? phone : ""), 200));
    }
}
