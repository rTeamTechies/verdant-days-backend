package com.verdantdays.model.dto;

import java.math.BigDecimal;

public class IncomeExpenseSummary {
    private BigDecimal income;
    private BigDecimal expenses;
    private BigDecimal profitOrLoss;

    public IncomeExpenseSummary(BigDecimal income, BigDecimal expenses) {
        this.income = income != null ? income : BigDecimal.ZERO;
        this.expenses = expenses != null ? expenses : BigDecimal.ZERO;
        this.profitOrLoss = this.income.subtract(this.expenses);
    }

    public BigDecimal getIncome() { return income; }
    public void setIncome(BigDecimal income) { this.income = income; }
    public BigDecimal getExpenses() { return expenses; }
    public void setExpenses(BigDecimal expenses) { this.expenses = expenses; }
    public BigDecimal getProfitOrLoss() { return profitOrLoss; }
    public void setProfitOrLoss(BigDecimal profitOrLoss) { this.profitOrLoss = profitOrLoss; }
}
