package com.verdantdays.controller;

import com.verdantdays.model.Customer;
import com.verdantdays.model.dto.ApiResponse;
import com.verdantdays.model.dto.CustomerRequest;
import com.verdantdays.service.ApartmentService;
import com.verdantdays.service.CustomerService;
import com.verdantdays.service.ExcelExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private ApartmentService apartmentService;

    @Autowired
    private ExcelExportService excelExportService;

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(required = false) Integer apartmentId,
            @RequestParam(required = false) Boolean subscribed,
            @RequestParam(required = false) String q) {
        var list = customerService.listForExport(apartmentId, subscribed, q);
        byte[] bytes = excelExportService.exportCustomers(list);
        String filename = "customers-" + LocalDate.now() + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @GetMapping
    public ResponseEntity<?> getAll(
            @RequestParam(required = false) Integer apartmentId,
            @RequestParam(required = false) Boolean subscribed,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        size = Math.min(Math.max(1, size), 100);
        page = Math.max(0, page);
        var pageResult = customerService.getPage(apartmentId, subscribed, q, page, size);
        return ResponseEntity.ok(new ApiResponse<>("OK", new com.verdantdays.model.dto.PageResponse<>(
                pageResult.getContent(), pageResult.getNumber(), pageResult.getSize(), pageResult.getTotalElements()), 200));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<Customer>>> search(@RequestParam String q) {
        return ResponseEntity.ok(new ApiResponse<>("OK", customerService.search(q), 200));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Customer>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(new ApiResponse<>("OK", customerService.getById(id), 200));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Customer>> create(@RequestBody CustomerRequest req) {
        if (req.getName() == null || req.getName().isBlank()) {
            return ResponseEntity.badRequest().body(new ApiResponse<>("Name is required", null, 400));
        }
        Customer c = toEntity(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>("Created", customerService.createOrUpdate(c), 201));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Customer>> update(@PathVariable Integer id, @RequestBody CustomerRequest req) {
        Customer c = toEntity(req);
        c.setId(id);
        return ResponseEntity.ok(new ApiResponse<>("OK", customerService.createOrUpdate(c), 200));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Integer id) {
        customerService.delete(id);
        return ResponseEntity.ok(new ApiResponse<>("Deleted", null, 200));
    }

    private Customer toEntity(CustomerRequest req) {
        Customer c = new Customer();
        c.setName(req.getName());
        String phone = req.getPhoneNumber();
        if (phone != null && !phone.isBlank()) {
            c.setPhoneNumber(phone.trim());
        } else {
            c.setPhoneNumber(null);
        }
        c.setAlternatePhone(req.getAlternatePhone());
        c.setEmail(req.getEmail());
        c.setAddress(req.getAddress());
        c.setFlatNumber(req.getFlatNumber());
        c.setSubscribedMonthly(req.getSubscribedMonthly() != null && req.getSubscribedMonthly());
        if (req.getApartmentId() != null) {
            c.setApartment(apartmentService.getById(req.getApartmentId()));
        }
        return c;
    }
}
