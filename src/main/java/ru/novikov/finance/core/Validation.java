package ru.novikov.finance.core;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Validation {

  private static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000000000");

  private Validation() {}

  public static String login(String login) {
    if (login == null || !login.trim().matches("[A-Za-z0-9_.-]{3,32}")) {
      throw new FinanceException("Логин: от 3 до 32 латинских букв, цифр или знаков _ . -");
    }
    return login.trim();
  }

  public static String category(String category) {
    if (category == null || category.isBlank()) {
      throw new FinanceException("Укажите категорию");
    }
    String value = category.trim();
    if (value.length() > 50) {
      throw new FinanceException("Название категории длиннее 50 символов");
    }
    return value;
  }

  public static BigDecimal amount(String text) {
    if (text == null || text.isBlank()) {
      throw new FinanceException("Укажите сумму");
    }
    BigDecimal value;
    try {
      value = new BigDecimal(text.trim().replace(',', '.'));
    } catch (NumberFormatException e) {
      throw new FinanceException("Сумма должна быть числом: " + text);
    }
    if (value.signum() <= 0) {
      throw new FinanceException("Сумма должна быть больше нуля");
    }
    if (value.compareTo(MAX_AMOUNT) > 0) {
      throw new FinanceException("Слишком большая сумма");
    }
    if (value.scale() > 2) {
      throw new FinanceException("У суммы не больше двух знаков после запятой");
    }
    return value.setScale(2, RoundingMode.UNNECESSARY);
  }
}
