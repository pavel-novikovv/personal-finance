package ru.novikov.finance.core;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class Wallet {

  private final String owner;
  private final List<Operation> operations = new ArrayList<>();
  private final Map<String, BigDecimal> budgets = new TreeMap<>();

  public Wallet(String owner) {
    this.owner = owner;
  }

  public String owner() {
    return owner;
  }

  public void add(Operation operation) {
    operations.add(operation);
  }

  public List<Operation> operations() {
    return Collections.unmodifiableList(operations);
  }

  public Map<String, BigDecimal> budgets() {
    return Collections.unmodifiableMap(budgets);
  }

  public void setBudget(String category, BigDecimal limit) {
    budgets.put(category, limit);
  }

  public boolean removeBudget(String category) {
    return budgets.remove(category) != null;
  }

  public void renameCategory(String from, String to) {
    for (int i = 0; i < operations.size(); i++) {
      Operation op = operations.get(i);
      if (op.category().equals(from)) {
        operations.set(i, new Operation(op.type(), to, op.amount(), op.date(), op.note()));
      }
    }
    BigDecimal budget = budgets.remove(from);
    if (budget != null) {
      budgets.merge(to, budget, BigDecimal::add);
    }
  }

  public BigDecimal total(OperationType type) {
    return operations.stream()
        .filter(op -> op.type() == type)
        .map(Operation::amount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  public BigDecimal balance() {
    return total(OperationType.INCOME).subtract(total(OperationType.EXPENSE));
  }

  public BigDecimal spentIn(String category) {
    return operations.stream()
        .filter(op -> op.type() == OperationType.EXPENSE && op.category().equals(category))
        .map(Operation::amount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  public boolean hasCategory(String category) {
    return budgets.containsKey(category)
        || operations.stream().anyMatch(op -> op.category().equals(category));
  }
}
