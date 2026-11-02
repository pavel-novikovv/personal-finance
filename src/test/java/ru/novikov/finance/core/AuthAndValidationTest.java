package ru.novikov.finance.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class AuthAndValidationTest {

  private final InMemoryUsers users = new InMemoryUsers();
  private final AuthService auth = new AuthService(users, new PasswordHasher());

  @Test
  void storesPasswordAsSaltedHash() {
    User user = auth.register("pavel", "secret".toCharArray());

    assertNotEquals("secret", user.passwordHash());
    assertEquals("pavel", auth.login("pavel", "secret".toCharArray()).login());
  }

  @Test
  void rejectsWrongPasswordAndUnknownUser() {
    auth.register("pavel", "secret".toCharArray());

    assertThrows(FinanceException.class, () -> auth.login("pavel", "wrong".toCharArray()));
    assertThrows(FinanceException.class, () -> auth.login("ghost", "secret".toCharArray()));
  }

  @Test
  void rejectsDuplicateLoginAndShortPassword() {
    auth.register("pavel", "secret".toCharArray());

    assertThrows(FinanceException.class, () -> auth.register("pavel", "other1".toCharArray()));
    assertThrows(FinanceException.class, () -> auth.register("anna", "123".toCharArray()));
    assertThrows(FinanceException.class, () -> auth.register("я", "secret".toCharArray()));
  }

  @Test
  void parsesAmountsWithDotOrComma() {
    assertEquals(new BigDecimal("12.50"), Validation.amount("12,5"));
    assertEquals(new BigDecimal("300.00"), Validation.amount("300"));
  }

  @Test
  void rejectsBadAmounts() {
    assertThrows(FinanceException.class, () -> Validation.amount("abc"));
    assertThrows(FinanceException.class, () -> Validation.amount("-5"));
    assertThrows(FinanceException.class, () -> Validation.amount("0"));
    assertThrows(FinanceException.class, () -> Validation.amount("1.234"));
    assertThrows(FinanceException.class, () -> Validation.amount(" "));
  }

  @Test
  void rejectsEmptyCategory() {
    assertThrows(FinanceException.class, () -> Validation.category("  "));
  }
}
