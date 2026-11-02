package ru.novikov.finance.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

final class InMemoryUsers implements UserRepository {

  private final Map<String, User> users = new HashMap<>();

  @Override
  public Optional<User> find(String login) {
    return Optional.ofNullable(users.get(login));
  }

  @Override
  public void save(User user) {
    users.put(user.login(), user);
  }
}
