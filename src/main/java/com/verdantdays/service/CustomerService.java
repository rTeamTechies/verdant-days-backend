package com.verdantdays.service;

import com.verdantdays.model.Customer;
import com.verdantdays.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ApartmentService apartmentService;

    public List<Customer> getAll() {
        return customerRepository.findByActiveTrueOrderByNameAsc();
    }

    public Page<Customer> getPage(Integer apartmentId, Boolean subscribed, String search, int page, int size) {
        Pageable p = PageRequest.of(page, size);
        if (search != null && !search.trim().isEmpty()) {
            return customerRepository.findAllActiveWithSearch(apartmentId, subscribed, search.trim(), p);
        }
        return customerRepository.findAllActive(apartmentId, subscribed, p);
    }

    /** All matching customers for Excel export (capped). */
    public List<Customer> listForExport(Integer apartmentId, Boolean subscribed, String search) {
        List<Customer> all = new java.util.ArrayList<>();
        int page = 0;
        Page<Customer> result;
        do {
            result = getPage(apartmentId, subscribed, search, page, 500);
            all.addAll(result.getContent());
            // touch apartment while session is open
            for (Customer c : result.getContent()) {
                if (c.getApartment() != null) {
                    c.getApartment().getName();
                }
            }
            page++;
        } while (page < result.getTotalPages() && all.size() < 20000);
        return all;
    }

    public List<Customer> getByApartment(Integer apartmentId) {
        return customerRepository.findByApartmentIdAndActiveTrueOrderByNameAsc(apartmentId);
    }

    public List<Customer> search(String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }
        List<Customer> found = customerRepository.searchByNameOrPhone(query.trim());
        return found.size() > 20 ? found.subList(0, 20) : found;
    }

    public Customer getById(Integer id) {
        return customerRepository.findById(id).orElseThrow(() -> new RuntimeException("Customer not found"));
    }

    public Customer createOrUpdate(Customer customer) {
        if (customer.getName() == null || customer.getName().isBlank()) {
            throw new RuntimeException("Name is required");
        }
        String name = customer.getName().trim();
        String phone = customer.getPhoneNumber();
        if (phone != null) {
            phone = phone.trim().replaceAll("\\s+", "");
            phone = phone.isEmpty() ? null : phone;
        }
        var apartment = (customer.getApartment() != null && customer.getApartment().getId() != null)
                ? apartmentService.getById(customer.getApartment().getId())
                : null;

        Customer target;
        if (customer.getId() != null) {
            // Merge onto managed entity so empty collections from the request never wipe orders
            target = getById(customer.getId());
        } else {
            target = new Customer();
            target.setActive(true);
        }
        target.setName(name);
        target.setPhoneNumber(phone);
        target.setAlternatePhone(customer.getAlternatePhone());
        target.setEmail(customer.getEmail());
        target.setAddress(customer.getAddress());
        target.setFlatNumber(customer.getFlatNumber());
        target.setSubscribedMonthly(customer.isSubscribedMonthly());
        target.setApartment(apartment);
        return customerRepository.save(target);
    }

    public void delete(Integer id) {
        Customer c = getById(id);
        c.setActive(false);
        customerRepository.save(c);
    }
}
