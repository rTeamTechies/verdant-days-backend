package com.verdantdays.model.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ExpenseRequest {
    private LocalDate expenseDate;
    private String description;
    private BigDecimal amount;
    private String paymentMode;

    public LocalDate getExpenseDate() { return expenseDate; }
    public void setExpenseDate(LocalDate expenseDate) { this.expenseDate = expenseDate; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getPaymentMode() { return paymentMode; }
    public void setPaymentMode(String paymentMode) { this.paymentMode = paymentMode; }
}
