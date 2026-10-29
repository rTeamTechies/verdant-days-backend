package com.verdantdays.service;

import com.verdantdays.model.*;
import com.verdantdays.model.dto.*;
import com.verdantdays.repository.ApartmentRepository;
import com.verdantdays.repository.CustomerRepository;
import com.verdantdays.repository.DeliverySlotRepository;
import com.verdantdays.repository.OrderRepository;
import com.verdantdays.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class StorefrontService {

    private static final int COURIER_DAYS = 2;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ApartmentRepository apartmentRepository;

    @Autowired
    private DeliverySlotRepository deliverySlotRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private StockService stockService;

    public List<CatalogProductDto> catalog() {
        return productRepository.findByActiveTrueOrderByOilTypeAscSizeAsc().stream()
                .map(p -> new CatalogProductDto(
                        p.getId(),
                        p.getOilType().name(),
                        p.getSize().name(),
                        p.getDisplayName(),
                        websitePrice(p)
                ))
                .toList();
    }

    public PromoValidateResponse validatePromo(String rawCode) {
        PromoValidateResponse res = new PromoValidateResponse();
        String code = normalizePromo(rawCode);
        if (code.isEmpty()) {
            res.setValid(false);
            res.setMessage("Enter your apartment code.");
            return res;
        }

        Apartment apartment = apartmentRepository.findByPromoCodeIgnoreCaseAndActiveTrue(code).orElse(null);
        if (apartment == null) {
            res.setValid(false);
            res.setMessage("This code is not active. Website orders are packed and couriered in 2 days at MRP.");
            return res;
        }

        DeliverySlot slot = nextSlot(apartment.getId()).orElse(null);
        if (slot == null) {
            res.setValid(false);
            res.setMessage("No drop is scheduled for " + apartment.getName() + " yet. Please try again later or order for courier at MRP.");
            return res;
        }

        res.setValid(true);
        res.setApartmentName(apartment.getName());
        res.setDeliveryDate(slot.getDeliveryDate());
        res.setMessage("Apartment price unlocked. Personal drop on the next scheduled date.");
        res.setPrices(productRepository.findByActiveTrueOrderByOilTypeAscSizeAsc().stream()
                .map(p -> {
                    BigDecimal mrp = websitePrice(p);
                    BigDecimal apt = p.getPrice();
                    return new PromoPriceDto(p.getId(), p.getDisplayName(), mrp, apt, mrp.subtract(apt).max(BigDecimal.ZERO));
                })
                .toList());
        return res;
    }

    @Transactional
    public CheckoutResponse checkout(CheckoutRequest req) {
        if (req == null) throw new RuntimeException("Order details are required");
        String name = trim(req.getName());
        String phone = normalizePhone(req.getPhoneNumber());
        if (name.isEmpty()) throw new RuntimeException("Name is required");
        if (phone.length() != 10) throw new RuntimeException("Enter a valid 10-digit phone number");
        if (req.getItems() == null || req.getItems().isEmpty()) throw new RuntimeException("Your cart is empty");

        String promo = normalizePromo(req.getPromoCode());
        boolean apartmentDrop = !promo.isEmpty();
        Apartment apartment = null;
        DeliverySlot slot = null;
        LocalDate deliveryDate;
        String shipping;

        if (apartmentDrop) {
            PromoValidateResponse promoRes = validatePromo(promo);
            if (!promoRes.isValid()) throw new RuntimeException(promoRes.getMessage());
            apartment = apartmentRepository.findByPromoCodeIgnoreCaseAndActiveTrue(promo).orElseThrow();
            slot = nextSlot(apartment.getId()).orElseThrow(() -> new RuntimeException("No drop is scheduled for this apartment yet"));
            String flat = trim(req.getFlatNumber());
            if (flat.isEmpty()) throw new RuntimeException("Flat / door number is required for apartment delivery");
            deliveryDate = slot.getDeliveryDate();
            shipping = apartment.getName() + ", Flat " + flat;
        } else {
            String address = trim(req.getAddress());
            if (address.isEmpty()) throw new RuntimeException("Delivery address is required");
            deliveryDate = LocalDate.now().plusDays(COURIER_DAYS);
            String city = trim(req.getCity());
            String pin = trim(req.getPincode());
            shipping = address + (city.isEmpty() ? "" : ", " + city) + (pin.isEmpty() ? "" : " - " + pin);
        }

        Customer customer = findOrCreateCustomer(name, phone, trim(req.getEmail()), apartment, req, shipping);
        Order order = new Order();
        order.setCustomer(customer);
        order.setOrderNumber(orderService.generateOrderNumber());
        order.setOrderType(Order.OrderType.WEBSITE);
        order.setOrderStatus(Order.OrderStatus.PLACED);
        order.setOrderSource(Order.OrderSource.WEBSITE);
        order.setFulfillmentType(apartmentDrop ? Order.FulfillmentType.APARTMENT : Order.FulfillmentType.COURIER);
        order.setDeliveryDate(deliveryDate);
        order.setPromoCode(apartmentDrop ? promo : null);
        order.setShippingAddress(shipping);
        order.setNotes(trim(req.getNotes()));
        order.setPaymentType("UPI".equalsIgnoreCase(trim(req.getPaymentType())) ? Order.PaymentType.UPI : Order.PaymentType.CASH);
        order.setPaymentStatus(Order.PaymentStatus.DUE);
        order.setAmountPaid(BigDecimal.ZERO);

        BigDecimal total = BigDecimal.ZERO;
        List<OrderItem> items = new ArrayList<>();
        for (CheckoutRequest.CheckoutItemRequest line : req.getItems()) {
            if (line == null || line.getProductId() == null) continue;
            int qty = line.getQuantity() == null ? 1 : line.getQuantity();
            if (qty < 1) continue;
            Product product = productRepository.findById(line.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));
            if (!product.isActive()) throw new RuntimeException(product.getDisplayName() + " is not available");
            BigDecimal unitPrice = apartmentDrop ? product.getPrice() : websitePrice(product);
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(qty));
            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setProduct(product);
            oi.setQuantity(qty);
            oi.setUnitPrice(unitPrice);
            oi.setSubtotal(subtotal);
            items.add(oi);
            total = total.add(subtotal);
        }
        if (items.isEmpty()) throw new RuntimeException("Your cart is empty");

        order.setItems(items);
        order.setSubtotalAmount(total);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setTotalAmount(total);
        Order saved = orderRepository.save(order);
        stockService.deductForOrder(saved);

        String message = apartmentDrop
                ? "Order placed. We will personally deliver to " + apartment.getName() + " on " + deliveryDate + "."
                : "Order placed. We will courier this in about " + COURIER_DAYS + " days.";

        return new CheckoutResponse(
                saved.getId(),
                saved.getOrderNumber(),
                saved.getTotalAmount(),
                deliveryDate,
                saved.getFulfillmentType().name(),
                apartmentDrop ? apartment.getName() : null,
                message
        );
    }

    private Customer findOrCreateCustomer(String name, String phone, String email, Apartment apartment,
                                         CheckoutRequest req, String shipping) {
        Customer customer = customerRepository.findFirstByPhoneNumberAndActiveTrueOrderByIdAsc(phone).orElse(null);
        if (customer == null) {
            customer = new Customer();
            customer.setPhoneNumber(phone);
            customer.setActive(true);
        }
        customer.setName(name);
        if (!email.isEmpty()) customer.setEmail(email);
        if (apartment != null) {
            customer.setApartment(apartment);
            customer.setFlatNumber(trim(req.getFlatNumber()));
            customer.setAddress(apartment.getAddress());
        } else {
            customer.setAddress(shipping);
        }
        return customerRepository.save(customer);
    }

    private java.util.Optional<DeliverySlot> nextSlot(Integer apartmentId) {
        return deliverySlotRepository.findFirstByApartmentIdAndActiveTrueAndDeliveryDateGreaterThanEqualOrderByDeliveryDateAsc(
                apartmentId, LocalDate.now());
    }

    static BigDecimal websitePrice(Product product) {
        return product.getMrp() != null ? product.getMrp() : product.getPrice();
    }

    static String normalizePromo(String code) {
        return code == null ? "" : code.trim().toUpperCase().replaceAll("\\s+", "");
    }

    static String normalizePhone(String phone) {
        if (phone == null) return "";
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() > 10) digits = digits.substring(digits.length() - 10);
        return digits;
    }

    static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
