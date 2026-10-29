package com.verdantdays;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.verdantdays.model.OrderItem;
import com.verdantdays.repository.CustomerRepository;
import com.verdantdays.repository.OrderRepository;
import com.verdantdays.repository.ProductRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Guards against destructive update bugs (cascade wipes, historical price mutation).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DataIntegrityIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired OrderRepository orderRepository;
    @Autowired CustomerRepository customerRepository;
    @Autowired ProductRepository productRepository;

    private String token;
    private int customerId;
    private int productId;
    private int orderId;
    private BigDecimal originalUnitPrice;
    private BigDecimal originalOrderTotal;
    private int apartmentId;

    @Test
    @org.junit.jupiter.api.Order(1)
    void login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"8015761437\",\"password\":\"Admin@123\"}"))
                .andExpect(status().isOk())
                .andReturn();
        token = objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    void setup_customer_product_order() throws Exception {
        MvcResult apt = mockMvc.perform(post("/api/apartments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Integrity Apt\",\"promoCode\":\"INTEGAPT\",\"active\":true}"))
                .andExpect(status().isCreated())
                .andReturn();
        apartmentId = objectMapper.readTree(apt.getResponse().getContentAsString()).get("data").get("id").asInt();

        MvcResult cust = mockMvc.perform(post("/api/customers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Integrity Customer\",\"phoneNumber\":\"9111000001\",\"apartmentId\":" + apartmentId + ",\"flatNumber\":\"A1\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        customerId = objectMapper.readTree(cust.getResponse().getContentAsString()).get("data").get("id").asInt();

        MvcResult products = mockMvc.perform(get("/api/products?page=0&size=1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode content = objectMapper.readTree(products.getResponse().getContentAsString()).get("data").get("content");
        Assertions.assertTrue(content.size() > 0, "Need at least one product");
        productId = content.get(0).get("id").asInt();
        originalUnitPrice = content.get(0).get("price").decimalValue();

        MvcResult order = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":" + customerId + ",\"paymentType\":\"CASH\",\"items\":[{\"productId\":" + productId + ",\"quantity\":2}]}"))
                .andExpect(status().isOk())
                .andReturn();
        orderId = objectMapper.readTree(order.getResponse().getContentAsString()).get("data").get("id").asInt();

        com.verdantdays.model.Order saved = orderRepository.findByIdWithItems(orderId).orElseThrow();
        originalOrderTotal = saved.getTotalAmount();
        Assertions.assertEquals(0, originalUnitPrice.compareTo(saved.getItems().get(0).getUnitPrice()));
        Assertions.assertEquals(2, saved.getItems().get(0).getQuantity());
    }

    @Test
    @org.junit.jupiter.api.Order(3)
    void customer_update_must_not_delete_orders() throws Exception {
        long ordersBefore = orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).size();
        Assertions.assertTrue(ordersBefore >= 1);

        mockMvc.perform(put("/api/customers/" + customerId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Integrity Customer Updated\",\"phoneNumber\":\"9111000001\",\"apartmentId\":" + apartmentId + ",\"flatNumber\":\"B2\",\"subscribedMonthly\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Integrity Customer Updated"));

        List<com.verdantdays.model.Order> ordersAfter = orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
        Assertions.assertEquals(ordersBefore, ordersAfter.size(), "Customer edit wiped orders");
        Assertions.assertTrue(ordersAfter.stream().anyMatch(o -> o.getId().equals(orderId)));

        com.verdantdays.model.Order stillThere = orderRepository.findByIdWithItems(orderId).orElseThrow();
        Assertions.assertEquals(0, originalOrderTotal.compareTo(stillThere.getTotalAmount()));
        Assertions.assertFalse(stillThere.getItems().isEmpty(), "Order items were deleted");
    }

    @Test
    @org.junit.jupiter.api.Order(4)
    void product_price_change_must_not_rewrite_existing_order_lines() throws Exception {
        BigDecimal newPrice = originalUnitPrice.add(new BigDecimal("99.00"));

        mockMvc.perform(put("/api/products/" + productId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"price\":" + newPrice + ",\"mrp\":" + newPrice.add(new BigDecimal("50")) + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.price").value(newPrice.doubleValue()));

        com.verdantdays.model.Order historical = orderRepository.findByIdWithItems(orderId).orElseThrow();
        OrderItem line = historical.getItems().get(0);
        Assertions.assertEquals(0, originalUnitPrice.compareTo(line.getUnitPrice()),
                "Existing order unitPrice must stay frozen after product price update");
        Assertions.assertEquals(0, originalOrderTotal.compareTo(historical.getTotalAmount()),
                "Existing order total must stay frozen after product price update");

        // New order should use the new catalog price
        MvcResult newOrderRes = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":" + customerId + ",\"paymentType\":\"UPI\",\"items\":[{\"productId\":" + productId + ",\"quantity\":1}]}"))
                .andExpect(status().isOk())
                .andReturn();
        int newOrderId = objectMapper.readTree(newOrderRes.getResponse().getContentAsString()).get("data").get("id").asInt();
        com.verdantdays.model.Order fresh = orderRepository.findByIdWithItems(newOrderId).orElseThrow();
        Assertions.assertEquals(0, newPrice.compareTo(fresh.getItems().get(0).getUnitPrice()),
                "New orders must pick up updated product price");
    }

    @Test
    @org.junit.jupiter.api.Order(5)
    void apartment_update_must_not_detach_or_delete_customers() throws Exception {
        Assertions.assertTrue(
                customerRepository.findByApartmentIdAndActiveTrueOrderByNameAsc(apartmentId).stream()
                        .anyMatch(c -> c.getId().equals(customerId)),
                "Customer should belong to apartment before update");

        mockMvc.perform(put("/api/apartments/" + apartmentId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Integrity Apt Renamed\",\"promoCode\":\"INTEGAPT\",\"address\":\"New Road\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Integrity Apt Renamed"));

        Assertions.assertTrue(
                customerRepository.findByApartmentIdAndActiveTrueOrderByNameAsc(apartmentId).stream()
                        .anyMatch(c -> c.getId().equals(customerId) && c.isActive()),
                "Apartment edit must not remove/detach customers");
        Assertions.assertFalse(orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).isEmpty(),
                "Apartment edit must not affect customer orders");
    }

    @Test
    @org.junit.jupiter.api.Order(6)
    void order_status_update_must_keep_line_prices() throws Exception {
        mockMvc.perform(put("/api/orders/" + orderId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentStatus\":\"PAID\",\"orderStatus\":\"DELIVERED\"}"))
                .andExpect(status().isOk());

        com.verdantdays.model.Order updated = orderRepository.findByIdWithItems(orderId).orElseThrow();
        Assertions.assertEquals(com.verdantdays.model.Order.PaymentStatus.PAID, updated.getPaymentStatus());
        Assertions.assertEquals(0, originalUnitPrice.compareTo(updated.getItems().get(0).getUnitPrice()));
        Assertions.assertEquals(0, originalOrderTotal.compareTo(updated.getTotalAmount()));
    }
}
