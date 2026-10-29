package com.verdantdays.controller;

import com.verdantdays.model.dto.ApiResponse;
import com.verdantdays.model.dto.PageResponse;
import com.verdantdays.model.dto.ReminderListItem;
import com.verdantdays.service.ReminderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reminders")
public class ReminderController {

    @Autowired
    private ReminderService reminderService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ReminderListItem>>> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String lastOrder,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        size = Math.min(Math.max(1, size), 100);
        page = Math.max(0, page);
        PageResponse<ReminderListItem> result = reminderService.getPage(type, lastOrder, page, size);
        return ResponseEntity.ok(new ApiResponse<>("OK", result, 200));
    }

    @PostMapping("/{customerId}/send")
    public ResponseEntity<ApiResponse<Map<String, Object>>> send(@PathVariable int customerId) {
        Map<String, Object> result = reminderService.sendReminder(customerId);
        return ResponseEntity.ok(new ApiResponse<>("Reminder sent", result, 200));
    }
}
