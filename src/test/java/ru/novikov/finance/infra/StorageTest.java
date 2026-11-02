package ru.novikov.finance.infra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.novikov.finance.core.FinanceException;
import ru.novikov.finance.core.Operation;
import ru.novikov.finance.core.OperationType;
import ru.novikov.finance.core.User;
import ru.novikov.finance.core.Wallet;

class StorageTest {

  @TempDir Path dir;

  private Wallet sampleWallet() {
    Wallet wallet = new Wallet("pavel");
    wallet.add(
        new Operation(
            OperationType.INCOME,
            "Зарплата",
            new BigDecimal("1000.00"),
            LocalDate.of(2026, 11, 1),
            ""));
    wallet.add(
        new Operation(
            OperationType.EXPENSE,
            "Кафе; бары",
            new BigDecimal("250.50"),
            LocalDate.of(2026, 11, 2),
            "ужин"));
    wallet.setBudget("Кафе; бары", new BigDecimal("500.00"));
    return wallet;
  }

  @Test
  void walletSurvivesSaveAndLoad() {
    FileWalletRepository repository = new FileWalletRepository(dir);
    repository.save(sampleWallet());

    Wallet loaded = repository.load("pavel").orElseThrow();

    assertEquals(2, loaded.operations().size());
    assertEquals(0, new BigDecimal("749.50").compareTo(loaded.balance()));
    assertEquals(0, new BigDecimal("500.00").compareTo(loaded.budgets().get("Кафе; бары")));
  }

  @Test
  void usersSurviveRestart() {
    Path file = dir.resolve("users.json");
    new FileUserRepository(file).save(new User("pavel", "salt", "hash"));

    assertEquals("hash", new FileUserRepository(file).find("pavel").orElseThrow().passwordHash());
  }

  @Test
  void csvExportEscapesSeparators() throws IOException {
    Path file = dir.resolve("out.csv");
    CsvExporter.write(sampleWallet(), file);

    List<String> lines = Files.readAllLines(file);

    assertEquals("date;type;category;amount;note", lines.get(0));
    assertTrue(lines.get(2).contains("\"Кафе; бары\";250.50;ужин"));
  }

  @Test
  void jsonExportCanBeImportedForAnotherOwner() {
    Path file = dir.resolve("snapshot.json");
    WalletJson.write(sampleWallet(), file);

    Wallet imported = WalletJson.read(file, "anna");

    assertEquals("anna", imported.owner());
    assertEquals(2, imported.operations().size());
  }

  @Test
  void brokenFileIsReported() throws IOException {
    Path file = dir.resolve("broken.json");
    Files.writeString(file, "{\"operations\":[{\"type\":\"GIFT\"}]}");

    assertThrows(FinanceException.class, () -> WalletJson.read(file, "pavel"));
  }
}
