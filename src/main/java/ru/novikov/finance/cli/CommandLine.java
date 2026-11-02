package ru.novikov.finance.cli;

import java.util.ArrayList;
import java.util.List;
import ru.novikov.finance.core.FinanceException;

final class CommandLine {

  private CommandLine() {}

  static List<String> split(String line) {
    List<String> words = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    boolean quoted = false;
    boolean hasWord = false;
    for (char ch : line.toCharArray()) {
      if (ch == '"') {
        quoted = !quoted;
        hasWord = true;
      } else if (Character.isWhitespace(ch) && !quoted) {
        if (hasWord) {
          words.add(current.toString());
          current.setLength(0);
          hasWord = false;
        }
      } else {
        current.append(ch);
        hasWord = true;
      }
    }
    if (quoted) {
      throw new FinanceException("Не закрыта кавычка");
    }
    if (hasWord) {
      words.add(current.toString());
    }
    return words;
  }
}
