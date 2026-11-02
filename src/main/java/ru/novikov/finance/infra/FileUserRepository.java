package ru.novikov.finance.infra;

import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import ru.novikov.finance.core.FinanceException;
import ru.novikov.finance.core.User;
import ru.novikov.finance.core.UserRepository;

public final class FileUserRepository implements UserRepository {

  private final Path file;
  private final Map<String, User> users = new TreeMap<>();

  public FileUserRepository(Path file) {
    this.file = file;
    if (Files.isRegularFile(file)) {
      try {
        users.putAll(
            WalletJson.MAPPER.readValue(file.toFile(), new TypeReference<Map<String, User>>() {}));
      } catch (IOException e) {
        throw new FinanceException("Не удалось прочитать список пользователей: " + e.getMessage());
      }
    }
  }

  @Override
  public Optional<User> find(String login) {
    return Optional.ofNullable(users.get(login));
  }

  @Override
  public void save(User user) {
    users.put(user.login(), user);
    try {
      Path parent = file.toAbsolutePath().getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      WalletJson.MAPPER.writeValue(file.toFile(), users);
    } catch (IOException e) {
      throw new FinanceException("Не удалось сохранить пользователей: " + e.getMessage());
    }
  }
}
