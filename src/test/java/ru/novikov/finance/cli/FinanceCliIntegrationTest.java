package ru.novikov.finance.cli;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.novikov.finance.core.AuthService;
import ru.novikov.finance.core.FinanceService;
import ru.novikov.finance.core.PasswordHasher;
import ru.novikov.finance.infra.FileUserRepository;
import ru.novikov.finance.infra.FileWalletRepository;

class FinanceCliIntegrationTest {

  @TempDir Path dir;

  private String session(String commands) throws IOException {
    AuthService auth =
        new AuthService(new FileUserRepository(dir.resolve("users.json")), new PasswordHasher());
    FinanceService finance =
        new FinanceService(
            new FileWalletRepository(dir.resolve("wallets")), auth, Clock.systemUTC());
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    PrintStream out = new PrintStream(buffer, true, StandardCharsets.UTF_8);
    new FinanceCli(auth, finance, new BufferedReader(new StringReader(commands)), out, null).run();
    return buffer.toString(StandardCharsets.UTF_8);
  }

  @Test
  void exampleFromTaskProducesExpectedSummary() throws IOException {
    String output =
        session(
            """
            register pavel secret
            login pavel secret
            expense 300 Еда
            expense 500 Еда
            expense 3000 Развлечения
            expense 3000 "Коммунальные услуги"
            expense 1500 Такси
            income 20000 Зарплата
            income 40000 Зарплата
            income 3000 Бонус
            budget Еда 4000
            budget Развлечения 3000
            budget "Коммунальные услуги" 2500
            summary
            exit
            """);

    assertTrue(output.contains("Общий доход: 63,000.00"));
    assertTrue(output.contains("Зарплата: 60,000.00"));
    assertTrue(output.contains("Общие расходы: 8,300.00"));
    assertTrue(
        output.contains(
            "Коммунальные услуги: 2,500.00, потрачено: 3,000.00, оставшийся бюджет: -500.00"));
    assertTrue(output.contains("Еда: 4,000.00, потрачено: 800.00, оставшийся бюджет: 3,200.00"));
    assertTrue(output.contains("[внимание] Превышен бюджет «Коммунальные услуги»"));
  }

  @Test
  void walletIsSavedOnExitAndLoadedOnLogin() throws IOException {
    session("register pavel secret\nlogin pavel secret\nincome 1000 Зарплата\nexit\n");

    String second = session("login pavel\nsecret\nsummary\nexit\n");

    assertTrue(second.contains("Операций в кошельке: 1"));
    assertTrue(second.contains("Общий доход: 1,000.00"));
  }

  @Test
  void transferBetweenUsers() throws IOException {
    session(
        "register pavel secret\nregister anna secret\nlogin pavel secret\nincome 1000 Зарплата\ntransfer anna 400\nexit\n");

    String anna = session("login anna secret\nsummary\nexit\n");

    assertTrue(anna.contains("Переводы: 400.00"));
  }

  @Test
  void invalidInputIsExplainedNotCrashing() throws IOException {
    String output =
        session(
            """
            income 100 Зарплата
            register pavel secret
            login pavel wrong
            login pavel secret
            expense abc Еда
            expense 100
            summary Спорт
            summary --from 2026-13-01
            foo
            exit
            """);

    assertTrue(output.contains("Сначала войдите"));
    assertTrue(output.contains("Неверный логин или пароль"));
    assertTrue(output.contains("Сумма должна быть числом"));
    assertTrue(output.contains("Формат: expense"));
    assertTrue(output.contains("Категория «Спорт» не найдена"));
    assertTrue(output.contains("ГГГГ-ММ-ДД"));
    assertTrue(output.contains("Неизвестная команда: foo"));
  }

  @Test
  void exportAndImportRoundTrip() throws IOException {
    Path json = dir.resolve("export.json");
    Path csv = dir.resolve("export.csv");
    session(
        "register pavel secret\nlogin pavel secret\nincome 500 Бонус\nexport "
            + json
            + "\nexport "
            + csv
            + "\nexit\n");

    String output =
        session("register anna secret\nlogin anna secret\nimport " + json + "\nsummary\nexit\n");

    assertTrue(Files.readString(csv).contains("INCOME;Бонус;500.00"));
    assertTrue(output.contains("Загружено операций: 1"));
    assertTrue(output.contains("Общий доход: 500.00"));
  }
}
