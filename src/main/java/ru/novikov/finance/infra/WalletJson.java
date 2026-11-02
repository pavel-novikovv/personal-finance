package ru.novikov.finance.infra;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import ru.novikov.finance.core.FinanceException;
import ru.novikov.finance.core.Operation;
import ru.novikov.finance.core.OperationType;
import ru.novikov.finance.core.Wallet;

public final class WalletJson {

  static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

  public record OperationDto(
      String type, String category, String amount, String date, String note) {}

  public record WalletDto(
      String owner, List<OperationDto> operations, Map<String, String> budgets) {}

  private WalletJson() {}

  public static WalletDto toDto(Wallet wallet) {
    List<OperationDto> operations =
        wallet.operations().stream()
            .map(
                op ->
                    new OperationDto(
                        op.type().name(),
                        op.category(),
                        op.amount().toPlainString(),
                        op.date().toString(),
                        op.note()))
            .toList();
    Map<String, String> budgets = new TreeMap<>();
    wallet.budgets().forEach((category, limit) -> budgets.put(category, limit.toPlainString()));
    return new WalletDto(wallet.owner(), operations, budgets);
  }

  public static Wallet fromDto(WalletDto dto, String owner) {
    Wallet wallet = new Wallet(owner);
    try {
      if (dto.operations() != null) {
        for (OperationDto op : dto.operations()) {
          wallet.add(
              new Operation(
                  OperationType.valueOf(op.type()),
                  op.category(),
                  new BigDecimal(op.amount()),
                  LocalDate.parse(op.date()),
                  op.note()));
        }
      }
      if (dto.budgets() != null) {
        dto.budgets()
            .forEach((category, limit) -> wallet.setBudget(category, new BigDecimal(limit)));
      }
    } catch (IllegalArgumentException | NullPointerException | DateTimeParseException e) {
      throw new FinanceException("Файл кошелька повреждён: " + e.getMessage());
    }
    return wallet;
  }

  public static void write(Wallet wallet, Path file) {
    try {
      Path parent = file.toAbsolutePath().getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      MAPPER.writeValue(file.toFile(), toDto(wallet));
    } catch (IOException e) {
      throw new FinanceException("Не удалось записать " + file + ": " + e.getMessage());
    }
  }

  public static Wallet read(Path file, String owner) {
    try {
      return fromDto(MAPPER.readValue(file.toFile(), WalletDto.class), owner);
    } catch (IOException e) {
      throw new FinanceException("Не удалось прочитать " + file + ": " + e.getMessage());
    }
  }
}
