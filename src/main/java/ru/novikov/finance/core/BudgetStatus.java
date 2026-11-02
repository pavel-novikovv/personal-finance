package ru.novikov.finance.core;

import java.math.BigDecimal;

public record BudgetStatus(String category, BigDecimal limit, BigDecimal spent) {

  public BigDecimal remaining() {
    return limit.subtract(spent);
  }
}
