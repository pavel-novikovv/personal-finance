package ru.novikov.finance;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import ru.novikov.finance.cli.FinanceCli;
import ru.novikov.finance.core.AuthService;
import ru.novikov.finance.core.FinanceService;
import ru.novikov.finance.core.PasswordHasher;
import ru.novikov.finance.infra.FileUserRepository;
import ru.novikov.finance.infra.FileWalletRepository;

public final class App {

  private App() {}

  public static void main(String[] args) throws IOException {
    Path dataDir = Path.of(args.length > 0 ? args[0] : "data");
    AuthService auth =
        new AuthService(
            new FileUserRepository(dataDir.resolve("users.json")), new PasswordHasher());
    FinanceService finance =
        new FinanceService(
            new FileWalletRepository(dataDir.resolve("wallets")), auth, Clock.systemDefaultZone());
    BufferedReader in =
        new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
    new FinanceCli(auth, finance, in, System.out, System.console()).run();
  }
}
