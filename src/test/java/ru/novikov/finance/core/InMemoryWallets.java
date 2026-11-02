package ru.novikov.finance.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

final class InMemoryWallets implements WalletRepository {

  final Map<String, Wallet> saved = new HashMap<>();

  @Override
  public Optional<Wallet> load(String owner) {
    return Optional.ofNullable(saved.get(owner));
  }

  @Override
  public void save(Wallet wallet) {
    saved.put(wallet.owner(), wallet);
  }
}
