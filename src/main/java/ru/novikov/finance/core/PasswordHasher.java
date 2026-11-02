package ru.novikov.finance.core;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordHasher {

  private static final int ITERATIONS = 120_000;
  private static final int KEY_BITS = 256;
  private final SecureRandom random = new SecureRandom();

  public String newSalt() {
    byte[] salt = new byte[16];
    random.nextBytes(salt);
    return Base64.getEncoder().encodeToString(salt);
  }

  public String hash(char[] password, String salt) {
    try {
      PBEKeySpec spec =
          new PBEKeySpec(password, Base64.getDecoder().decode(salt), ITERATIONS, KEY_BITS);
      byte[] hash =
          SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
      spec.clearPassword();
      return Base64.getEncoder().encodeToString(hash);
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("Не удалось вычислить хэш пароля", e);
    }
  }

  public boolean matches(char[] password, String salt, String expectedHash) {
    byte[] actual = Base64.getDecoder().decode(hash(password, salt));
    return MessageDigest.isEqual(actual, Base64.getDecoder().decode(expectedHash));
  }
}
