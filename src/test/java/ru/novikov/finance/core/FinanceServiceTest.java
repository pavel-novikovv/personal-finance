package ru.novikov.finance.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FinanceServiceTest {

  private final Clock clock = Clock.fixed(Instant.parse("2026-11-02T10:00:00Z"), ZoneOffset.UTC);
  private final InMemoryWallets wallets = new InMemoryWallets();
  private AuthService auth;
  private FinanceService service;
  private Wallet wallet;

  @BeforeEach
  void setUp() {
    auth = new AuthService(new InMemoryUsers(), new PasswordHasher());
    auth.register("pavel", "secret".toCharArray());
    auth.register("anna", "secret".toCharArray());
    service = new FinanceService(wallets, auth, clock);
    wallet = service.openWallet("pavel");
  }

  private static BigDecimal money(String value) {
    return new BigDecimal(value);
  }

  @Test
  void reproducesExampleFromTask() {
    service.addExpense(wallet, "Еда", "300", "");
    service.addExpense(wallet, "Еда", "500", "");
    service.addExpense(wallet, "Развлечения", "3000", "");
    service.addExpense(wallet, "Коммунальные услуги", "3000", "");
    service.addExpense(wallet, "Такси", "1500", "");
    service.addIncome(wallet, "Зарплата", "20000", "");
    service.addIncome(wallet, "Зарплата", "40000", "");
    service.addIncome(wallet, "Бонус", "3000", "");
    service.setBudget(wallet, "Еда", "4000");
    service.setBudget(wallet, "Развлечения", "3000");
    service.setBudget(wallet, "Коммунальные услуги", "2500");

    Report report = service.report(wallet, ReportFilter.ALL);

    assertEquals(0, money("63000").compareTo(report.totalIncome()));
    assertEquals(0, money("60000").compareTo(report.incomeByCategory().get("Зарплата")));
    assertEquals(0, money("8300").compareTo(report.totalExpense()));
    Map<String, BigDecimal> remaining =
        report.budgets().stream()
            .collect(
                java.util.stream.Collectors.toMap(BudgetStatus::category, BudgetStatus::remaining));
    assertEquals(0, money("-500").compareTo(remaining.get("Коммунальные услуги")));
    assertEquals(0, money("3200").compareTo(remaining.get("Еда")));
    assertEquals(0, money("0").compareTo(remaining.get("Развлечения")));
  }

  @Test
  void warnsWhenBudgetExceeded() {
    service.addIncome(wallet, "Зарплата", "10000", "");
    service.setBudget(wallet, "Еда", "1000");

    List<String> alerts = service.addExpense(wallet, "Еда", "1200", "");

    assertTrue(alerts.stream().anyMatch(a -> a.contains("Превышен бюджет «Еда»")));
  }

  @Test
  void warnsAtEightyPercentOfBudget() {
    service.addIncome(wallet, "Зарплата", "10000", "");
    service.setBudget(wallet, "Еда", "1000");

    List<String> alerts = service.addExpense(wallet, "Еда", "800", "");

    assertTrue(alerts.stream().anyMatch(a -> a.contains("больше 80%")));
  }

  @Test
  void warnsWhenExpensesExceedIncome() {
    service.addIncome(wallet, "Зарплата", "100", "");

    List<String> alerts = service.addExpense(wallet, "Такси", "150", "");

    assertTrue(alerts.stream().anyMatch(a -> a.contains("превысили доходы")));
  }

  @Test
  void reportFiltersBySeveralCategoriesAndReportsMissingOnes() {
    service.addExpense(wallet, "Еда", "300", "");
    service.addExpense(wallet, "Такси", "200", "");
    service.addExpense(wallet, "Кино", "500", "");

    Report report =
        service.report(wallet, new ReportFilter(Set.of("Еда", "Такси", "Спорт"), null, null));

    assertEquals(0, money("500").compareTo(report.totalExpense()));
    assertEquals(List.of("Спорт"), report.missingCategories());
  }

  @Test
  void reportFiltersByPeriod() {
    wallet.add(
        new Operation(OperationType.EXPENSE, "Еда", money("100"), LocalDate.of(2026, 10, 1), ""));
    wallet.add(
        new Operation(OperationType.EXPENSE, "Еда", money("200"), LocalDate.of(2026, 11, 1), ""));

    Report report =
        service.report(wallet, new ReportFilter(Set.of(), LocalDate.of(2026, 11, 1), null));

    assertEquals(0, money("200").compareTo(report.totalExpense()));
  }

  @Test
  void transferCreatesExpenseForSenderAndIncomeForRecipient() {
    service.addIncome(wallet, "Зарплата", "1000", "");

    service.transfer(wallet, "anna", "300");

    assertEquals(0, money("700").compareTo(wallet.balance()));
    assertEquals(0, money("300").compareTo(wallets.saved.get("anna").balance()));
  }

  @Test
  void transferValidatesRecipientAndBalance() {
    service.addIncome(wallet, "Зарплата", "100", "");

    assertThrows(FinanceException.class, () -> service.transfer(wallet, "nobody", "10"));
    assertThrows(FinanceException.class, () -> service.transfer(wallet, "pavel", "10"));
    assertThrows(FinanceException.class, () -> service.transfer(wallet, "anna", "500"));
  }

  @Test
  void renameCategoryMovesOperationsAndBudget() {
    service.addExpense(wallet, "Еда", "300", "");
    service.setBudget(wallet, "Еда", "1000");

    service.renameCategory(wallet, "Еда", "Продукты");

    assertEquals(0, money("300").compareTo(wallet.spentIn("Продукты")));
    assertTrue(wallet.budgets().containsKey("Продукты"));
    assertThrows(FinanceException.class, () -> service.renameCategory(wallet, "Нет такой", "X"));
  }

  @Test
  void removingMissingBudgetIsReported() {
    assertThrows(FinanceException.class, () -> service.removeBudget(wallet, "Еда"));
  }
}
