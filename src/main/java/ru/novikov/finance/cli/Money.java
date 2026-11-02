package ru.novikov.finance.cli;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

final class Money {

  private Money() {}

  static String format(BigDecimal value) {
    return new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US)).format(value);
  }
}
