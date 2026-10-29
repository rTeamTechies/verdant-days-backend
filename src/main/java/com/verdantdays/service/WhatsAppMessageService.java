package com.verdantdays.service;

import com.verdantdays.model.BillSettings;
import com.verdantdays.model.Order;
import com.verdantdays.model.OrderItem;
import com.verdantdays.model.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.stream.Collectors;

@Service
public class WhatsAppMessageService {

    @Autowired
    private BillSettingsService billSettingsService;

    public String buildOrderConfirmationMessage(Order order) {
        BillSettings settings = billSettingsService.get();
        String template = settings.getOrderWhatsappTemplate();
        if (template == null || template.isBlank()) {
            template = BillSettings.DEFAULT_ORDER_WHATSAPP;
        }

        String orderNum = order.getOrderNumber() != null ? order.getOrderNumber() : ("#" + order.getId());
        String customerName = order.getCustomer() != null && order.getCustomer().getName() != null
                ? order.getCustomer().getName() : "Customer";
        String items = buildItemsBlock(order);
        BigDecimal discount = order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO;
        String discountLine = discount.compareTo(BigDecimal.ZERO) > 0
                ? "🏷️ Discount: ₹ " + money(discount) + "\n" : "";

        String payLabel = order.getPaymentStatus() == Order.PaymentStatus.PAID ? "Paid ✅"
                : order.getPaymentStatus() == Order.PaymentStatus.PARTIAL_PAID ? "Partial Paid" : "Due";
        String paymentType = order.getPaymentType() == Order.PaymentType.UPI ? "UPI" : "Cash";

        String amountLine;
        if (order.getPaymentStatus() == Order.PaymentStatus.DUE) {
            amountLine = "💰 Amount Due: ₹ " + money(order.getTotalAmount());
        } else if (order.getPaymentStatus() == Order.PaymentStatus.PARTIAL_PAID) {
            BigDecimal paid = order.getAmountPaid() != null ? order.getAmountPaid() : BigDecimal.ZERO;
            BigDecimal due = order.getTotalAmount().subtract(paid);
            amountLine = "💰 Paid: ₹ " + money(paid) + " | Due: ₹ " + money(due);
        } else {
            amountLine = "💰 Amount Paid: ₹ " + money(order.getAmountPaid() != null ? order.getAmountPaid() : order.getTotalAmount());
        }

        return template
                .replace("{{orderNumber}}", orderNum)
                .replace("{{customerName}}", customerName)
                .replace("{{items}}", items)
                .replace("{{discountLine}}", discountLine)
                .replace("{{amountLine}}", amountLine)
                .replace("{{total}}", money(order.getTotalAmount()))
                .replace("{{discount}}", money(discount))
                .replace("{{paymentType}}", paymentType)
                .replace("{{paymentStatus}}", payLabel)
                .replace("{{amountPaid}}", money(order.getAmountPaid() != null ? order.getAmountPaid() : BigDecimal.ZERO));
    }

    private String buildItemsBlock(Order order) {
        if (order.getItems() == null || order.getItems().isEmpty()) return "-";
        return order.getItems().stream()
                .filter(oi -> oi.getProduct() != null)
                .map(oi -> {
                    Product p = oi.getProduct();
                    String name = p.getDisplayName() != null ? p.getDisplayName() : "Item";
                    String desc = p.getDescription() != null && !p.getDescription().isBlank()
                            ? " (" + p.getDescription().trim() + ")" : "";
                    return "• " + name + desc + " x" + oi.getQuantity() + " — ₹" + money(oi.getSubtotal());
                })
                .collect(Collectors.joining("\n"));
    }

    private static String money(BigDecimal v) {
        if (v == null) return "0.00";
        return String.format("%.2f", v.doubleValue());
    }

    public String getReminderMessage(boolean subscribed) {
        BillSettings settings = billSettingsService.get();
        if (subscribed) {
            String t = settings.getReminderSubscribedTemplate();
            return (t == null || t.isBlank()) ? BillSettings.DEFAULT_REMINDER_SUBSCRIBED : t;
        }
        String t = settings.getReminderNonSubscribedTemplate();
        return (t == null || t.isBlank()) ? BillSettings.DEFAULT_REMINDER_NON_SUBSCRIBED : t;
    }

    public String getWhatsAppUrl(String phoneNumber, String message) {
        String cleaned = phoneNumber == null ? "" : phoneNumber.replaceAll("[^0-9]", "");
        if (!cleaned.startsWith("91") && cleaned.length() == 10) {
            cleaned = "91" + cleaned;
        }
        String encoded = java.net.URLEncoder.encode(message, java.nio.charset.StandardCharsets.UTF_8);
        return "https://wa.me/" + cleaned + "?text=" + encoded;
    }
}
