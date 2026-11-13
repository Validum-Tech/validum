package ru.validum.core.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Детерминированный идентификатор человека.
 *
 * <p>{@code personId} не случайное: он выводится из seed, которым сгенерирован профиль.
 * Один и тот же {@code --seed} всегда даёт один и тот же идентификатор — в CLI, REST и
 * спецификации набора. В батче seed профиля равен базовому плюс индекс, поэтому
 * «seed + номер строки» и «seed одного профиля» дают совпадающие идентификаторы.
 *
 * <p>Формат: {@code pd-} и 16 символов sha-256 в hex — коротко для CSV и достаточно
 * длинно, чтобы внутри одного набора коллизий не было.
 */
public final class PersonIds {

  private static final String PREFIX = "pd-";
  private static final String SALT = "validum-person:";
  private static final int HEX_LENGTH = 16;

  private PersonIds() {}

  /** Идентификатор для профиля, сгенерированного этим seed. */
  public static String of(long seed) {
    byte[] hash = sha256(SALT + seed);
    StringBuilder hex = new StringBuilder(PREFIX);
    for (int i = 0; i < HEX_LENGTH / 2; i++) {
      hex.append(Character.forDigit((hash[i] >> 4) & 0xF, 16));
      hex.append(Character.forDigit(hash[i] & 0xF, 16));
    }
    return hex.toString();
  }

  /** Похоже ли значение на идентификатор, который выдаёт {@link #of(long)}. */
  public static boolean isValid(String personId) {
    if (personId == null || !personId.startsWith(PREFIX) || personId.length() != PREFIX.length() + HEX_LENGTH) {
      return false;
    }
    for (int i = PREFIX.length(); i < personId.length(); i++) {
      if (Character.digit(personId.charAt(i), 16) < 0) {
        return false;
      }
    }
    return true;
  }

  private static byte[] sha256(String value) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 недоступен", e);
    }
  }
}
