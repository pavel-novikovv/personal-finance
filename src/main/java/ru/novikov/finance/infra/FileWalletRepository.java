package ru.novikov.finance.infra;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import ru.novikov.finance.core.Wallet;
import ru.novikov.finance.core.WalletRepository;

public final class FileWalletRepository implements WalletRepository {

  private final Path directory;

  public FileWalletRepository(Path directory) {
    this.directory = directory;
  }

  @Override
  public Optional<Wallet> load(String owner) {
    Path file = fileOf(owner);
    return Files.isRegularFile(file) ? Optional.of(WalletJson.read(file, owner)) : Optional.empty();
  }

  @Override
  public void save(Wallet wallet) {
    WalletJson.write(wallet, fileOf(wallet.owner()));
  }

  private Path fileOf(String owner) {
    return directory.resolve(owner + ".json");
  }
}
