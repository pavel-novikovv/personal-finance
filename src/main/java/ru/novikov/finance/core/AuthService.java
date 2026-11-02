package ru.novikov.finance.core;

public final class AuthService {

  private final UserRepository users;
  private final PasswordHasher hasher;

  public AuthService(UserRepository users, PasswordHasher hasher) {
    this.users = users;
    this.hasher = hasher;
  }

  public User register(String login, char[] password) {
    String name = Validation.login(login);
    if (password == null || password.length < 4) {
      throw new FinanceException("Пароль должен быть не короче 4 символов");
    }
    if (users.find(name).isPresent()) {
      throw new FinanceException("Пользователь " + name + " уже существует");
    }
    String salt = hasher.newSalt();
    User user = new User(name, salt, hasher.hash(password, salt));
    users.save(user);
    return user;
  }

  public User login(String login, char[] password) {
    String name = Validation.login(login);
    User user =
        users.find(name).orElseThrow(() -> new FinanceException("Неверный логин или пароль"));
    if (!hasher.matches(password, user.salt(), user.passwordHash())) {
      throw new FinanceException("Неверный логин или пароль");
    }
    return user;
  }

  public boolean exists(String login) {
    return users.find(login).isPresent();
  }
}
