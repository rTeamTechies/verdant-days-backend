package com.verdantdays;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end flow tests for Verdant Days API.
 * Tests: Auth, Customers, Apartments, Products, Orders, Dashboard.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String accessToken;

    @Test
    @Order(1)
    void login_success() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"8015761437\",\"password\":\"Admin@123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        this.accessToken = body.get("accessToken").asText();
        Assertions.assertNotNull(this.accessToken);
    }

    @Test
    @Order(2)
    void login_failure_invalidCredentials() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"8015761437\",\"password\":\"wrong\"}"))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.message").value("Bad credentials"));
    }

    @Test
    @Order(3)
    void getProducts_requiresAuth() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(4)
    void getProducts_success() throws Exception {
        mockMvc.perform(get("/api/products")
                        .header("Authorization", "Bearer " + this.accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    @Test
    @Order(5)
    void getApartments_success() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/apartments")
                        .header("Authorization", "Bearer " + this.accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("content");
        Assertions.assertTrue(data.isArray() && data.size() >= 0);
    }

    @Test
    @Order(6)
    void getCustomers_paginated() throws Exception {
        mockMvc.perform(get("/api/customers?page=0&size=20")
                        .header("Authorization", "Bearer " + this.accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.page").exists())
                .andExpect(jsonPath("$.data.totalElements").exists());
    }

    @Test
    @Order(7)
    void getCustomers_filterBySubscribed() throws Exception {
        mockMvc.perform(get("/api/customers?subscribed=true&page=0&size=20")
                        .header("Authorization", "Bearer " + this.accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @Order(8)
    void createOrder_success() throws Exception {
        MvcResult custResult = mockMvc.perform(post("/api/customers")
                        .header("Authorization", "Bearer " + this.accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test Customer\",\"phoneNumber\":\"9000000001\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        int customerId = objectMapper.readTree(custResult.getResponse().getContentAsString())
                .get("data").get("id").asInt();

        MvcResult prodResult = mockMvc.perform(get("/api/products")
                        .header("Authorization", "Bearer " + this.accessToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode products = objectMapper.readTree(prodResult.getResponse().getContentAsString()).get("data").get("content");
        Assertions.assertTrue(products.size() > 0, "Expected seeded catalog products");
        int productId = products.get(0).get("id").asInt();

        String orderBody = String.format(
                "{\"customerId\":%d,\"paymentType\":\"CASH\",\"items\":[{\"productId\":%d,\"quantity\":2}]}",
                customerId, productId);

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.orderNumber").exists());
    }

    @Test
    @Order(9)
    void searchOrders_paginated() throws Exception {
        mockMvc.perform(get("/api/orders?page=0&size=10")
                        .header("Authorization", "Bearer " + this.accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalPages").exists());
    }

    @Test
    @Order(10)
    void searchOrders_filterByPaymentType() throws Exception {
        mockMvc.perform(get("/api/orders?paymentType=CASH&page=0&size=10")
                        .header("Authorization", "Bearer " + this.accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @Order(11)
    void getOrdersByCustomer() throws Exception {
        MvcResult custResult = mockMvc.perform(get("/api/customers?page=0&size=1")
                        .header("Authorization", "Bearer " + this.accessToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode custData = objectMapper.readTree(custResult.getResponse().getContentAsString()).get("data").get("content");
        if (custData.size() == 0) return;
        int customerId = custData.get(0).get("id").asInt();

        mockMvc.perform(get("/api/orders/customer/" + customerId)
                        .header("Authorization", "Bearer " + this.accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @Order(12)
    void getDashboardStats() throws Exception {
        String startDate = "2025-01-01";
        String endDate = "2025-12-31";
        mockMvc.perform(get("/api/dashboard/stats?startDate=" + startDate + "&endDate=" + endDate)
                        .header("Authorization", "Bearer " + this.accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalSales").exists())
                .andExpect(jsonPath("$.data.orderCount").exists())
                .andExpect(jsonPath("$.data.totalLitresSold").exists())
                .andExpect(jsonPath("$.data.oilWiseLitresSold").exists())
                .andExpect(jsonPath("$.data.productWiseSales").exists())
                .andExpect(jsonPath("$.data.salesByDate").exists());
    }

    @Test
    @Order(13)
    void getApartmentDetails() throws Exception {
        MvcResult aptResult = mockMvc.perform(get("/api/apartments")
                        .header("Authorization", "Bearer " + this.accessToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode apts = objectMapper.readTree(aptResult.getResponse().getContentAsString()).get("data");
        if (!apts.isArray() || apts.size() == 0) return;
        int aptId = apts.get(0).get("id").asInt();

        mockMvc.perform(get("/api/apartments/" + aptId + "/details")
                        .header("Authorization", "Bearer " + this.accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.apartment").exists())
                .andExpect(jsonPath("$.data.customers").isArray())
                .andExpect(jsonPath("$.data.customerCount").exists());
    }

    @Test
    @Order(14)
    void getOrderWhatsAppMessage() throws Exception {
        MvcResult ordResult = mockMvc.perform(get("/api/orders?page=0&size=1")
                        .header("Authorization", "Bearer " + this.accessToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode orders = objectMapper.readTree(ordResult.getResponse().getContentAsString()).get("data").get("content");
        if (!orders.isArray() || orders.size() == 0) return;
        int orderId = orders.get(0).get("id").asInt();

        mockMvc.perform(get("/api/orders/" + orderId + "/whatsapp-message")
                        .header("Authorization", "Bearer " + this.accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").exists())
                .andExpect(jsonPath("$.data.url").exists())
                .andExpect(jsonPath("$.data.phone").exists());
    }

    @Test
    @Order(15)
    void publicCatalog_noAuth() throws Exception {
        mockMvc.perform(get("/api/public/catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(6)))
                .andExpect(jsonPath("$.data[0].mrp").exists())
                .andExpect(jsonPath("$.data[0].price").doesNotExist());
    }

    @Test
    @Order(16)
    void publicPromo_invalid() throws Exception {
        mockMvc.perform(post("/api/public/promo/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"NOPE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid").value(false));
    }

    @Test
    @Order(17)
    void publicPromo_valid() throws Exception {
        MvcResult aptResult = mockMvc.perform(post("/api/apartments")
                        .header("Authorization", "Bearer " + this.accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Happinest Test\",\"address\":\"Kalavakkam\",\"promoCode\":\"HAPPINEST\",\"active\":true}"))
                .andExpect(status().isCreated())
                .andReturn();
        int apartmentId = objectMapper.readTree(aptResult.getResponse().getContentAsString())
                .get("data").get("id").asInt();

        String slotDate = java.time.LocalDate.now().plusDays(10).toString();
        mockMvc.perform(post("/api/apartments/" + apartmentId + "/delivery-slots")
                        .header("Authorization", "Bearer " + this.accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deliveryDate\":\"" + slotDate + "\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/public/promo/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"HAPPINEST\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid").value(true))
                .andExpect(jsonPath("$.data.deliveryDate").exists())
                .andExpect(jsonPath("$.data.prices").isArray());
    }

    @Test
    @Order(18)
    void publicCheckout_courier() throws Exception {
        MvcResult catalog = mockMvc.perform(get("/api/public/catalog"))
                .andExpect(status().isOk())
                .andReturn();
        int productId = objectMapper.readTree(catalog.getResponse().getContentAsString()).get("data").get(0).get("id").asInt();
        String body = String.format(
                "{\"name\":\"Website Buyer\",\"phoneNumber\":\"9000011111\",\"address\":\"12 Test Street\",\"city\":\"Chennai\",\"pincode\":\"600001\",\"paymentType\":\"UPI\",\"items\":[{\"productId\":%d,\"quantity\":1}]}",
                productId);
        mockMvc.perform(post("/api/public/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderNumber").exists())
                .andExpect(jsonPath("$.data.fulfillmentType").value("COURIER"))
                .andExpect(jsonPath("$.data.deliveryDate").exists());
    }

    @Test
    @Order(19)
    void publicCheckout_apartmentPromo() throws Exception {
        // Ensure promo apartment + slot exist (also created in publicPromo_valid)
        MvcResult validate = mockMvc.perform(post("/api/public/promo/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"HAPPINEST\"}"))
                .andExpect(status().isOk())
                .andReturn();
        boolean valid = objectMapper.readTree(validate.getResponse().getContentAsString())
                .get("data").get("valid").asBoolean();
        Assertions.assertTrue(valid, "HAPPINEST promo should be valid before apartment checkout");

        MvcResult catalog = mockMvc.perform(get("/api/public/catalog"))
                .andExpect(status().isOk())
                .andReturn();
        int productId = objectMapper.readTree(catalog.getResponse().getContentAsString()).get("data").get(0).get("id").asInt();
        String body = String.format(
                "{\"name\":\"Apartment Buyer\",\"phoneNumber\":\"9000022222\",\"promoCode\":\"HAPPINEST\",\"flatNumber\":\"A101\",\"paymentType\":\"CASH\",\"items\":[{\"productId\":%d,\"quantity\":1}]}",
                productId);
        mockMvc.perform(post("/api/public/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderNumber").exists())
                .andExpect(jsonPath("$.data.fulfillmentType").value("APARTMENT"))
                .andExpect(jsonPath("$.data.apartmentName").exists());
    }
}
