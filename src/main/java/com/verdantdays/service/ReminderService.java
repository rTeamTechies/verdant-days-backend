package com.verdantdays.service;

import com.verdantdays.model.Customer;
import com.verdantdays.model.dto.PageResponse;
import com.verdantdays.model.dto.ReminderListItem;
import com.verdantdays.repository.CustomerRepository;
import com.verdantdays.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReminderService {

    private static final int DAYS_THRESHOLD = 20;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private WhatsAppMessageService whatsAppMessageService;

    public List<ReminderListItem> getReminderEligibleCustomers() {
        List<Customer> allActive = customerRepository.findByActiveTrueOrderByNameAsc();
        Map<Integer, LocalDateTime> lastOrderByCustomer = orderRepository.findLastOrderDateByCustomer().stream()
                .collect(Collectors.toMap(row -> (Integer) row[0], row -> (LocalDateTime) row[1]));

        LocalDate cutoff = LocalDate.now().minusDays(DAYS_THRESHOLD);
        List<ReminderListItem> result = new ArrayList<>();

        for (Customer c : allActive) {
            LocalDateTime lastOrder = lastOrderByCustomer.get(c.getId());
            LocalDate lastOrderDate = lastOrder != null ? lastOrder.toLocalDate() : null;
            boolean eligible = lastOrderDate == null || lastOrderDate.isBefore(cutoff);

            if (eligible) {
                String type = c.isSubscribedMonthly() ? "Subscribed (>20 days)" : "Non-subscribed (>20 days)";
                result.add(new ReminderListItem(
                        c.getId(),
                        c.getName(),
                        c.getPhoneNumber(),
                        type,
                        lastOrderDate,
                        c.getLastReminderSentAt()
                ));
            }
        }

        return result;
    }

    public PageResponse<ReminderListItem> getPage(String type, String lastOrder, int page, int size) {
        List<ReminderListItem> filtered = getReminderEligibleCustomers();
        if (type != null && !type.isBlank() && !"all".equalsIgnoreCase(type)) {
            filtered = filtered.stream()
                    .filter(r -> type.equals(r.reminderType()))
                    .collect(Collectors.toList());
        }
        if ("never".equalsIgnoreCase(lastOrder)) {
            filtered = filtered.stream()
                    .filter(r -> r.lastOrderDate() == null)
                    .collect(Collectors.toList());
        }
        long total = filtered.size();
        int from = Math.min(page * size, filtered.size());
        int to = Math.min(from + size, filtered.size());
        return new PageResponse<>(filtered.subList(from, to), page, size, total);
    }

    @Transactional
    public Map<String, Object> sendReminder(int customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        if (customer.getPhoneNumber() == null || customer.getPhoneNumber().isBlank()) {
            throw new RuntimeException("Customer has no phone number for WhatsApp");
        }
        String message = whatsAppMessageService.getReminderMessage(customer.isSubscribedMonthly());
        String url = whatsAppMessageService.getWhatsAppUrl(customer.getPhoneNumber(), message);
        customer.setLastReminderSentAt(LocalDateTime.now());
        customerRepository.save(customer);
        return Map.of("url", url, "message", message, "phone", customer.getPhoneNumber(),
                "lastReminderSentAt", customer.getLastReminderSentAt().toString());
    }
}
