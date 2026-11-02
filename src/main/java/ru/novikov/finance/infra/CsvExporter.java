package ru.novikov.finance.infra;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import ru.novikov.finance.core.FinanceException;
import ru.novikov.finance.core.Operation;
import ru.novikov.finance.core.Wallet;

public final class CsvExporter {

  private CsvExporter() {}

  public static void write(Wallet wallet, Path file) {
    List<String> lines = new ArrayList<>();
    lines.add("date;type;category;amount;note");
    for (Operation op : wallet.operations()) {
      lines.add(
          String.join(
              ";",
              op.date().toString(),
              op.type().name(),
              escape(op.category()),
              op.amount().toPlainString(),
              escape(op.note())));
    }
    try {
      Path parent = file.toAbsolutePath().getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      Files.write(file, lines, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new FinanceException("Не удалось записать " + file + ": " + e.getMessage());
    }
  }

  private static String escape(String value) {
    if (value.contains(";") || value.contains("\"") || value.contains("\n")) {
      return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
  }
}
