package ru.novikov.finance.core;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Operation(
    OperationType type, String category, BigDecimal amount, LocalDate date, String note) {

  public Operation {
    if (type == null || category == null || category.isBlank() || amount == null || date == null) {
      throw new IllegalArgumentException("У операции должны быть тип, категория, сумма и дата");
    }
    if (amount.signum() <= 0) {
      throw new IllegalArgumentException("Сумма операции должна быть больше нуля");
    }
    note = note == null ? "" : note;
  }
}
