package ru.mindils.jb2.app.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Утильный класс для извлечения чистого JSON из ответов LLM.
 * LLM часто возвращает JSON обернутый в markdown блоки кода.
 */
public class JsonExtractor {
  private static final Logger log = LoggerFactory.getLogger(JsonExtractor.class);

  // Паттерны для поиска JSON в разных форматах
  private static final Pattern MARKDOWN_JSON_BLOCK = Pattern.compile(
      "```(?:json)?\\s*([\\s\\S]*?)```",
      Pattern.CASE_INSENSITIVE | Pattern.DOTALL
  );

  private static final Pattern JSON_OBJECT = Pattern.compile(
      "\\{[\\s\\S]*\\}",
      Pattern.DOTALL
  );

  private static final Pattern JSON_ARRAY = Pattern.compile(
      "\\[[\\s\\S]*\\]",
      Pattern.DOTALL
  );

  /**
   * Извлекает чистый JSON из ответа LLM.
   * Пробует различные стратегии извлечения в порядке приоритета.
   *
   * @param rawResponse сырой ответ от LLM
   * @return очищенный JSON или исходная строка если JSON не найден
   */
  public static String extractJson(String rawResponse) {
    if (rawResponse == null || rawResponse.trim().isEmpty()) {
      log.warn("Empty or null response provided to JsonExtractor");
      return rawResponse;
    }

    String trimmed = rawResponse.trim();
    log.debug("Extracting JSON from response of length: {}", trimmed.length());

    // Стратегия 1: Проверяем, может это уже чистый JSON
    if (isValidJsonStart(trimmed)) {
      log.debug("Response already starts with valid JSON");
      return trimmed;
    }

    // Стратегия 2: Ищем JSON в markdown блоке кода
    String fromMarkdown = extractFromMarkdownBlock(trimmed);
    if (fromMarkdown != null) {
      log.debug("Successfully extracted JSON from markdown block");
      return fromMarkdown;
    }

    // Стратегия 3: Ищем JSON объект или массив в тексте
    String fromPattern = extractFromPattern(trimmed);
    if (fromPattern != null) {
      log.debug("Successfully extracted JSON using pattern matching");
      return fromPattern;
    }

    // Стратегия 4: Возвращаем исходную строку
    log.warn("Could not extract JSON from response, returning original");
    return trimmed;
  }

  /**
   * Проверяет, начинается ли строка с валидного JSON
   */
  private static boolean isValidJsonStart(String str) {
    if (str == null || str.isEmpty()) {
      return false;
    }
    char firstChar = str.charAt(0);
    return firstChar == '{' || firstChar == '[';
  }

  /**
   * Извлекает JSON из markdown блока кода
   * Поддерживает форматы: ```json ... ```, ``` ... ```
   */
  private static String extractFromMarkdownBlock(String text) {
    Matcher matcher = MARKDOWN_JSON_BLOCK.matcher(text);
    if (matcher.find()) {
      String extracted = matcher.group(1).trim();
      if (!extracted.isEmpty()) {
        return extracted;
      }
    }
    return null;
  }

  /**
   * Извлекает JSON используя паттерны для поиска объектов и массивов
   */
  private static String extractFromPattern(String text) {
    // Сначала пробуем найти JSON объект
    Matcher objectMatcher = JSON_OBJECT.matcher(text);
    if (objectMatcher.find()) {
      String extracted = objectMatcher.group().trim();
      if (isBalanced(extracted)) {
        return extracted;
      }
    }

    // Потом пробуем найти JSON массив
    Matcher arrayMatcher = JSON_ARRAY.matcher(text);
    if (arrayMatcher.find()) {
      String extracted = arrayMatcher.group().trim();
      if (isBalanced(extracted)) {
        return extracted;
      }
    }

    return null;
  }

  /**
   * Проверяет, что скобки в JSON сбалансированы
   */
  private static boolean isBalanced(String json) {
    int braceCount = 0;
    int bracketCount = 0;
    boolean inString = false;
    boolean escaped = false;

    for (char c : json.toCharArray()) {
      if (escaped) {
        escaped = false;
        continue;
      }

      if (c == '\\') {
        escaped = true;
        continue;
      }

      if (c == '"') {
        inString = !inString;
        continue;
      }

      if (inString) {
        continue;
      }

      switch (c) {
        case '{':
          braceCount++;
          break;
        case '}':
          braceCount--;
          break;
        case '[':
          bracketCount++;
          break;
        case ']':
          bracketCount--;
          break;
      }
    }

    return braceCount == 0 && bracketCount == 0 && !inString;
  }

  /**
   * Удаляет все markdown блоки кода из текста
   * Полезно для очистки текста перед обработкой
   */
  public static String removeMarkdownBlocks(String text) {
    if (text == null || text.trim().isEmpty()) {
      return text;
    }
    return MARKDOWN_JSON_BLOCK.matcher(text).replaceAll("");
  }

  /**
   * Проверяет, содержит ли текст markdown блок кода
   */
  public static boolean containsMarkdownBlock(String text) {
    if (text == null || text.trim().isEmpty()) {
      return false;
    }
    return MARKDOWN_JSON_BLOCK.matcher(text).find();
  }
}