package ru.mindils.jb2.app.util;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * Утильный класс для генерации детерминированных UUID v5 на основе входящих параметров
 * Использует SHA-1 алгоритм для создания детерминированных идентификаторов
 * Без namespace - одинаковые параметры создают одинаковые UUID в любых приложениях
 */
@Component
public class UuidGenerator {

  /**
   * Генерирует UUID v5 на основе входящих параметров
   * Использует SHA-1 хэш для создания детерминированного результата
   *
   * @param params параметры для генерации UUID
   * @return детерминированный UUID v5
   */
  public UUID generateUuid(Object... params) {
    if (params == null || params.length == 0) {
      throw new IllegalArgumentException("Parameters cannot be null or empty");
    }

    try {
      // Объединяем все параметры в одну строку через запятую
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < params.length; i++) {
        if (i > 0) {
          sb.append(",");
        }
        sb.append(params[i] != null ? params[i].toString() : "null");
      }

      String input = sb.toString();

      // Создаем UUID v5 напрямую из строки параметров
      return generateUuidV5FromString(input);

    } catch (Exception e) {
      throw new RuntimeException("Failed to generate UUID v5", e);
    }
  }

  /**
   * Генерирует UUID версии 5 (name-based с SHA-1) напрямую из строки
   *
   * @param input строка для генерации UUID
   * @return UUID v5
   */
  private UUID generateUuidV5FromString(String input) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-1");

      // Хэшируем входную строку в UTF-8
      byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));

      // Создаем UUID из первых 16 байт хэша (из 20 байт SHA-1)
      return createUuidV5FromBytes(hash);

    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-1 algorithm not available", e);
    }
  }

  /**
   * Создает UUID v5 из массива байт (использует первые 16 байт из 20-байтного SHA-1)
   */
  private UUID createUuidV5FromBytes(byte[] hash) {
    long mostSigBits = 0;
    long leastSigBits = 0;

    // Первые 8 байт для mostSignificantBits
    for (int i = 0; i < 8; i++) {
      mostSigBits = (mostSigBits << 8) | (hash[i] & 0xff);
    }

    // Следующие 8 байт для leastSignificantBits
    for (int i = 8; i < 16; i++) {
      leastSigBits = (leastSigBits << 8) | (hash[i] & 0xff);
    }

    // Устанавливаем версию UUID как 5 (name-based with SHA-1)
    mostSigBits &= ~(0xF000L);  // Очищаем биты версии (биты 12-15)
    mostSigBits |= (0x5000L);   // Устанавливаем версию 5

    // Устанавливаем variant bits (биты 62-63 в leastSigBits)
    leastSigBits &= ~(0xC000000000000000L);  // Очищаем variant bits
    leastSigBits |= (0x8000000000000000L);   // Устанавливаем variant (10 binary)

    return new UUID(mostSigBits, leastSigBits);
  }
}