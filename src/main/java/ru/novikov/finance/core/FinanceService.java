package ru.novikov.finance.core;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class FinanceService {

  private static final BigDecimal WARNING_SHARE = new BigDecimal("0.80");

  private final WalletRepository wallets;
  private final AuthService auth;
  private final Clock clock;

  public FinanceService(WalletRepository wallets, AuthService auth, Clock clock) {
    this.wallets = wallets;
    this.auth = auth;
    this.clock = clock;
  }

  public Wallet openWallet(String owner) {
    return wallets.load(owner).orElseGet(() -> new Wallet(owner));
  }

  public void saveWallet(Wallet wallet) {
    wallets.save(wallet);
  }

  public List<String> addIncome(Wallet wallet, String category, String amount, String note) {
    wallet.add(
        new Operation(
            OperationType.INCOME,
            Validation.category(category),
            Validation.amount(amount),
            LocalDate.now(clock),
            note));
    return alerts(wallet, null);
  }

  public List<String> addExpense(Wallet wallet, String category, String amount, String note) {
    String name = Validation.category(category);
    wallet.add(
        new Operation(
            OperationType.EXPENSE, name, Validation.amount(amount), LocalDate.now(clock), note));
    return alerts(wallet, name);
  }

  public List<String> setBudget(Wallet wallet, String category, String amount) {
    String name = Validation.category(category);
    wallet.setBudget(name, Validation.amount(amount));
    return alerts(wallet, name);
  }

  public void removeBudget(Wallet wallet, String category) {
    if (!wallet.removeBudget(Validation.category(category))) {
      throw new FinanceException("Бюджет для категории " + category + " не задан");
    }
  }

  public void renameCategory(Wallet wallet, String from, String to) {
    String source = Validation.category(from);
    String target = Validation.category(to);
    if (!wallet.hasCategory(source)) {
      throw new FinanceException("Категория " + source + " не найдена");
    }
    if (source.equals(target)) {
      throw new FinanceException("Новое название совпадает со старым");
    }
    wallet.renameCategory(source, target);
  }

  public List<String> transfer(Wallet from, String recipientLogin, String amount) {
    String recipient = Validation.login(recipientLogin);
    if (recipient.equals(from.owner())) {
      throw new FinanceException("Нельзя перевести деньги самому себе");
    }
    if (!auth.exists(recipient)) {
      throw new FinanceException("Пользователь " + recipient + " не найден");
    }
    BigDecimal value = Validation.amount(amount);
    if (value.compareTo(from.balance()) > 0) {
      throw new FinanceException("Недостаточно средств: баланс " + from.balance());
    }
    LocalDate today = LocalDate.now(clock);
    from.add(
        new Operation(OperationType.EXPENSE, "Переводы", value, today, "получатель " + recipient));
    Wallet target = openWallet(recipient);
    target.add(
        new Operation(
            OperationType.INCOME, "Переводы", value, today, "отправитель " + from.owner()));
    wallets.save(target);
    wallets.save(from);
    return alerts(from, null);
  }

  public Report report(Wallet wallet, ReportFilter filter) {
    Map<String, BigDecimal> income = new TreeMap<>();
    Map<String, BigDecimal> expense = new TreeMap<>();
    for (Operation op : wallet.operations()) {
      if (filter.accepts(op)) {
        Map<String, BigDecimal> target = op.type() == OperationType.INCOME ? income : expense;
        target.merge(op.category(), op.amount(), BigDecimal::add);
      }
    }
    List<BudgetStatus> budgets = new ArrayList<>();
    for (Map.Entry<String, BigDecimal> budget : wallet.budgets().entrySet()) {
      if (filter.categories().isEmpty() || filter.categories().contains(budget.getKey())) {
        budgets.add(
            new BudgetStatus(
                budget.getKey(),
                budget.getValue(),
                expense.getOrDefault(budget.getKey(), BigDecimal.ZERO)));
      }
    }
    List<String> missing =
        filter.categories().stream().filter(c -> !wallet.hasCategory(c)).sorted().toList();
    return new Report(sum(income), sum(expense), income, expense, budgets, missing);
  }

  public List<String> alerts(Wallet wallet, String category) {
    List<String> alerts = new ArrayList<>();
    if (category != null && wallet.budgets().containsKey(category)) {
      BigDecimal limit = wallet.budgets().get(category);
      BigDecimal spent = wallet.spentIn(category);
      if (spent.compareTo(limit) > 0) {
        alerts.add(
            "Превышен бюджет «"
                + category
                + "»: потрачено "
                + spent
                + " из "
                + limit
                + ", перерасход "
                + spent.subtract(limit));
      } else if (spent.compareTo(limit.multiply(WARNING_SHARE).setScale(2, RoundingMode.HALF_UP))
          >= 0) {
        alerts.add(
            "Израсходовано больше 80% бюджета «" + category + "»: " + spent + " из " + limit);
      }
    }
    BigDecimal income = wallet.total(OperationType.INCOME);
    BigDecimal expense = wallet.total(OperationType.EXPENSE);
    if (expense.compareTo(income) > 0) {
      alerts.add("Расходы (" + expense + ") превысили доходы (" + income + ")");
    } else if (expense.signum() > 0 && expense.compareTo(income) == 0) {
      alerts.add("Баланс равен нулю: все доходы потрачены");
    }
    return alerts;
  }

  private static BigDecimal sum(Map<String, BigDecimal> values) {
    return values.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
  }
}
