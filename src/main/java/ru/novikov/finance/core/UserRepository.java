package ru.novikov.finance.core;

import java.util.Optional;

public interface UserRepository {

  Optional<User> find(String login);

  void save(User user);
}
