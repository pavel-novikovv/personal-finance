package ru.novikov.finance.cli;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import ru.novikov.finance.core.AuthService;
import ru.novikov.finance.core.BudgetStatus;
import ru.novikov.finance.core.FinanceException;
import ru.novikov.finance.core.FinanceService;
import ru.novikov.finance.core.Operation;
import ru.novikov.finance.core.Report;
import ru.novikov.finance.core.ReportFilter;
import ru.novikov.finance.core.Wallet;
import ru.novikov.finance.infra.CsvExporter;
import ru.novikov.finance.infra.WalletJson;

public final class FinanceCli {

  private final AuthService auth;
  private final FinanceService finance;
  private final BufferedReader in;
  private final PrintStream out;
  private final Console console;
  private Wallet wallet;

  public FinanceCli(
      AuthService auth,
      FinanceService finance,
      BufferedReader in,
      PrintStream out,
      Console console) {
    this.auth = auth;
    this.finance = finance;
    this.in = in;
    this.out = out;
    this.console = console;
  }

  public void run() throws IOException {
    out.println("Личные финансы. Введите help, чтобы увидеть команды.");
    while (true) {
      out.print(wallet == null ? "> " : wallet.owner() + "> ");
      out.flush();
      String line = in.readLine();
      if (line == null) {
        logout();
        return;
      }
      try {
        List<String> words = CommandLine.split(line.trim());
        if (words.isEmpty()) {
          continue;
        }
        if (words.get(0).equalsIgnoreCase("exit")) {
          logout();
          out.println("До свидания");
          return;
        }
        execute(words);
      } catch (FinanceException e) {
        out.println("Ошибка: " + e.getMessage());
      }
    }
  }

  private void execute(List<String> words) throws IOException {
    String command = words.get(0).toLowerCase();
    switch (command) {
      case "help" -> help();
      case "register" -> register(words);
      case "login" -> login(words);
      case "logout" -> {
        requireLogin();
        logout();
      }
      case "income", "expense" -> operation(command, words);
      case "budget" -> budget(words);
      case "budget-remove" -> budgetRemove(words);
      case "rename" -> rename(words);
      case "summary" -> summary(words);
      case "history" -> history();
      case "transfer" -> transfer(words);
      case "export" -> export(words);
      case "import" -> importWallet(words);
      case "save" -> {
        requireLogin();
        finance.saveWallet(wallet);
        out.println("Кошелёк сохранён");
      }
      default -> out.println("Неизвестная команда: " + words.get(0) + ". Введите help.");
    }
  }

  private void help() {
    out.println(
        """
        Команды:
          register <логин> [пароль]          регистрация
          login <логин> [пароль]             вход (без пароля в строке он будет запрошен отдельно)
          logout                             выход из кошелька с сохранением
          income <сумма> <категория> [заметка]   доход, например: income 20000 Зарплата
          expense <сумма> <категория> [заметка]  расход, например: expense 3000 "Коммунальные услуги"
          budget <категория> <сумма>         задать или изменить бюджет категории
          budget-remove <категория>          убрать бюджет
          rename <старая> <новая>            переименовать категорию
          summary [категории...] [--from ГГГГ-ММ-ДД] [--to ГГГГ-ММ-ДД] [--file путь]
                                             сводка; можно выбрать категории, период и записать в файл
          history                            все операции
          transfer <логин> <сумма>           перевод другому пользователю
          export <файл.json|файл.csv>        выгрузить кошелёк
          import <файл.json>                 загрузить снимок кошелька вместо текущих данных
          save                               сохранить сейчас
          exit                               выйти с сохранением
        Категории из нескольких слов пишите в кавычках.""");
  }

  private void register(List<String> words) throws IOException {
    if (words.size() < 2 || words.size() > 3) {
      out.println("Формат: register <логин> [пароль]");
      return;
    }
    char[] password = password(words);
    auth.register(words.get(1), password);
    out.println("Пользователь " + words.get(1) + " зарегистрирован. Войдите командой login.");
  }

  private void login(List<String> words) throws IOException {
    if (words.size() < 2 || words.size() > 3) {
      out.println("Формат: login <логин> [пароль]");
      return;
    }
    if (wallet != null) {
      logout();
    }
    char[] password = password(words);
    String login = auth.login(words.get(1), password).login();
    wallet = finance.openWallet(login);
    out.println(
        "Добро пожаловать, "
            + login
            + ". Операций в кошельке: "
            + wallet.operations().size()
            + ", баланс: "
            + Money.format(wallet.balance()));
  }

  private char[] password(List<String> words) throws IOException {
    if (words.size() == 3) {
      return words.get(2).toCharArray();
    }
    if (console != null) {
      char[] typed = console.readPassword("Пароль: ");
      return typed == null ? new char[0] : typed;
    }
    out.print("Пароль: ");
    out.flush();
    String line = in.readLine();
    return line == null ? new char[0] : line.toCharArray();
  }

  private void logout() {
    if (wallet != null) {
      finance.saveWallet(wallet);
      out.println("Кошелёк " + wallet.owner() + " сохранён");
      wallet = null;
    }
  }

  private void operation(String command, List<String> words) {
    requireLogin();
    if (words.size() < 3 || words.size() > 4) {
      out.println("Формат: " + command + " <сумма> <категория> [заметка]");
      return;
    }
    String note = words.size() == 4 ? words.get(3) : "";
    List<String> alerts =
        command.equals("income")
            ? finance.addIncome(wallet, words.get(2), words.get(1), note)
            : finance.addExpense(wallet, words.get(2), words.get(1), note);
    out.println(
        (command.equals("income") ? "Доход" : "Расход")
            + " записан. Баланс: "
            + Money.format(wallet.balance()));
    printAlerts(alerts);
  }

  private void budget(List<String> words) {
    requireLogin();
    if (words.size() != 3) {
      out.println("Формат: budget <категория> <сумма>");
      return;
    }
    List<String> alerts = finance.setBudget(wallet, words.get(1), words.get(2));
    out.println(
        "Бюджет «"
            + words.get(1).trim()
            + "» установлен: "
            + Money.format(wallet.budgets().get(words.get(1).trim())));
    printAlerts(alerts);
  }

  private void budgetRemove(List<String> words) {
    requireLogin();
    if (words.size() != 2) {
      out.println("Формат: budget-remove <категория>");
      return;
    }
    finance.removeBudget(wallet, words.get(1));
    out.println("Бюджет «" + words.get(1) + "» удалён");
  }

  private void rename(List<String> words) {
    requireLogin();
    if (words.size() != 3) {
      out.println("Формат: rename <старая категория> <новая категория>");
      return;
    }
    finance.renameCategory(wallet, words.get(1), words.get(2));
    out.println("Категория «" + words.get(1) + "» переименована в «" + words.get(2) + "»");
  }

  private void summary(List<String> words) {
    requireLogin();
    Set<String> categories = new HashSet<>();
    LocalDate from = null;
    LocalDate to = null;
    Path file = null;
    for (int i = 1; i < words.size(); i++) {
      String word = words.get(i);
      if (word.equals("--from") || word.equals("--to") || word.equals("--file")) {
        if (i + 1 >= words.size()) {
          throw new FinanceException("После " + word + " нужно значение");
        }
        String value = words.get(++i);
        switch (word) {
          case "--from" -> from = date(value);
          case "--to" -> to = date(value);
          default -> file = Path.of(value);
        }
      } else {
        categories.add(word);
      }
    }
    Report report = finance.report(wallet, new ReportFilter(categories, from, to));
    String text = render(report);
    if (file == null) {
      out.print(text);
    } else {
      try {
        Files.writeString(file, text);
        out.println("Сводка записана в " + file);
      } catch (IOException e) {
        throw new FinanceException("Не удалось записать " + file + ": " + e.getMessage());
      }
    }
  }

  private String render(Report report) {
    StringBuilder text = new StringBuilder();
    for (String missing : report.missingCategories()) {
      text.append("Категория «").append(missing).append("» не найдена\n");
    }
    text.append("Общий доход: ").append(Money.format(report.totalIncome())).append('\n');
    appendCategories(text, "Доходы по категориям:", report.incomeByCategory());
    text.append("Общие расходы: ").append(Money.format(report.totalExpense())).append('\n');
    appendCategories(text, "Расходы по категориям:", report.expenseByCategory());
    if (!report.budgets().isEmpty()) {
      text.append("Бюджет по категориям:\n");
      for (BudgetStatus budget : report.budgets()) {
        text.append("  ")
            .append(budget.category())
            .append(": ")
            .append(Money.format(budget.limit()))
            .append(", потрачено: ")
            .append(Money.format(budget.spent()))
            .append(", оставшийся бюджет: ")
            .append(Money.format(budget.remaining()))
            .append('\n');
      }
    }
    text.append("Баланс: ").append(Money.format(report.balance())).append('\n');
    return text.toString();
  }

  private void appendCategories(StringBuilder text, String title, Map<String, BigDecimal> values) {
    if (values.isEmpty()) {
      return;
    }
    text.append(title).append('\n');
    values.forEach(
        (category, sum) ->
            text.append("  ").append(category).append(": ").append(Money.format(sum)).append('\n'));
  }

  private void history() {
    requireLogin();
    if (wallet.operations().isEmpty()) {
      out.println("Операций пока нет");
      return;
    }
    for (Operation op : wallet.operations()) {
      out.printf(
          "%s  %-7s %-22s %15s  %s%n",
          op.date(),
          op.type().name().equals("INCOME") ? "доход" : "расход",
          op.category(),
          Money.format(op.amount()),
          op.note());
    }
  }

  private void transfer(List<String> words) {
    requireLogin();
    if (words.size() != 3) {
      out.println("Формат: transfer <логин получателя> <сумма>");
      return;
    }
    List<String> alerts = finance.transfer(wallet, words.get(1), words.get(2));
    out.println("Переведено " + words.get(2) + " пользователю " + words.get(1));
    printAlerts(alerts);
  }

  private void export(List<String> words) {
    requireLogin();
    if (words.size() != 2) {
      out.println("Формат: export <файл.json|файл.csv>");
      return;
    }
    Path file = Path.of(words.get(1));
    if (words.get(1).toLowerCase().endsWith(".csv")) {
      CsvExporter.write(wallet, file);
    } else if (words.get(1).toLowerCase().endsWith(".json")) {
      WalletJson.write(wallet, file);
    } else {
      throw new FinanceException("Поддерживаются файлы .json и .csv");
    }
    out.println("Кошелёк выгружен в " + file);
  }

  private void importWallet(List<String> words) {
    requireLogin();
    if (words.size() != 2 || !words.get(1).toLowerCase().endsWith(".json")) {
      out.println("Формат: import <файл.json>");
      return;
    }
    Path file = Path.of(words.get(1));
    if (!Files.isRegularFile(file)) {
      throw new FinanceException("Файл не найден: " + file);
    }
    wallet = WalletJson.read(file, wallet.owner());
    out.println(
        "Загружено операций: "
            + wallet.operations().size()
            + ", бюджетов: "
            + wallet.budgets().size());
  }

  private LocalDate date(String value) {
    try {
      return LocalDate.parse(value);
    } catch (DateTimeParseException e) {
      throw new FinanceException("Дата должна быть в формате ГГГГ-ММ-ДД: " + value);
    }
  }

  private void requireLogin() {
    if (wallet == null) {
      throw new FinanceException("Сначала войдите: login <логин>");
    }
  }

  private void printAlerts(List<String> alerts) {
    for (String alert : alerts) {
      out.println("[внимание] " + alert);
    }
  }
}
