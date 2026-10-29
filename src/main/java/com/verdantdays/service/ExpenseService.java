package com.verdantdays.service;

import com.verdantdays.model.Expense;
import com.verdantdays.model.dto.ExpenseRequest;
import com.verdantdays.model.dto.IncomeExpenseSummary;
import com.verdantdays.repository.ExpenseRepository;
import com.verdantdays.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
public class ExpenseService {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private OrderRepository orderRepository;

    public Page<Expense> getPage(LocalDate startDate, LocalDate endDate, int page, int size) {
        Pageable p = PageRequest.of(page, Math.min(size, 50));
        return expenseRepository.findByDateRange(startDate, endDate, p);
    }

    public Expense create(ExpenseRequest req) {
        Expense e = new Expense();
        e.setExpenseDate(req.getExpenseDate() != null ? req.getExpenseDate() : LocalDate.now());
        e.setDescription(req.getDescription() != null ? req.getDescription().trim() : "");
        e.setAmount(req.getAmount() != null ? req.getAmount() : java.math.BigDecimal.ZERO);
        e.setPaymentMode(parsePaymentMode(req.getPaymentMode()));
        return expenseRepository.save(e);
    }

    public Expense update(Integer id, ExpenseRequest req) {
        Expense e = expenseRepository.findById(id).orElseThrow(() -> new RuntimeException("Expense not found"));
        if (req.getExpenseDate() != null) e.setExpenseDate(req.getExpenseDate());
        if (req.getDescription() != null) e.setDescription(req.getDescription().trim());
        if (req.getAmount() != null) e.setAmount(req.getAmount());
        if (req.getPaymentMode() != null) e.setPaymentMode(parsePaymentMode(req.getPaymentMode()));
        return expenseRepository.save(e);
    }

    public void delete(Integer id) {
        expenseRepository.deleteById(id);
    }

    public IncomeExpenseSummary getSummary(LocalDate startDate, LocalDate endDate) {
        BigDecimal income = BigDecimal.ZERO;
        if (startDate != null && endDate != null) {
            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.atTime(LocalTime.MAX);
            BigDecimal orderSum = orderRepository.sumTotalAmountByCreatedAtBetween(start, end);
            income = orderSum != null ? orderSum : BigDecimal.ZERO;
        }
        BigDecimal expensesSum = expenseRepository.sumAmountByDateRange(startDate, endDate);
        BigDecimal expenses = expensesSum != null ? expensesSum : BigDecimal.ZERO;
        return new IncomeExpenseSummary(income, expenses);
    }

    private static Expense.PaymentMode parsePaymentMode(String s) {
        if (s == null || s.isBlank()) return Expense.PaymentMode.CASH;
        if ("UPI".equalsIgnoreCase(s.trim())) return Expense.PaymentMode.UPI;
        return Expense.PaymentMode.CASH;
    }
}
