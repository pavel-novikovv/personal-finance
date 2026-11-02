package ru.novikov.finance.core;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record Report(
    BigDecimal totalIncome,
    BigDecimal totalExpense,
    Map<String, BigDecimal> incomeByCategory,
    Map<String, BigDecimal> expenseByCategory,
    List<BudgetStatus> budgets,
    List<String> missingCategories) {

  public BigDecimal balance() {
    return totalIncome.subtract(totalExpense);
  }
}
