package ru.novikov.finance.core;

import java.time.LocalDate;
import java.util.Set;

public record ReportFilter(Set<String> categories, LocalDate from, LocalDate to) {

  public static final ReportFilter ALL = new ReportFilter(Set.of(), null, null);

  public ReportFilter {
    categories = categories == null ? Set.of() : Set.copyOf(categories);
    if (from != null && to != null && from.isAfter(to)) {
      throw new FinanceException("Начало периода позже конца");
    }
  }

  boolean accepts(Operation operation) {
    return (categories.isEmpty() || categories.contains(operation.category()))
        && (from == null || !operation.date().isBefore(from))
        && (to == null || !operation.date().isAfter(to));
  }
}
