package ru.novikov.finance.core;

import java.util.Optional;

public interface WalletRepository {

  Optional<Wallet> load(String owner);

  void save(Wallet wallet);
}
