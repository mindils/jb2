package ru.mindils.jb2.app.service.util;

import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class HtmlToMarkdownConverter {

  private static final Logger log = LoggerFactory.getLogger(HtmlToMarkdownConverter.class);

  private final FlexmarkHtmlConverter converter;

  public HtmlToMarkdownConverter() {
    MutableDataSet options = new MutableDataSet();

    // Настройки для оптимального преобразования в Markdown для LLM
    options.set(FlexmarkHtmlConverter.SKIP_HEADING_1, false);
    options.set(FlexmarkHtmlConverter.SKIP_HEADING_2, false);
    options.set(FlexmarkHtmlConverter.SKIP_HEADING_3, false);
    options.set(FlexmarkHtmlConverter.SKIP_INLINE_STRONG, false);
    options.set(FlexmarkHtmlConverter.SKIP_INLINE_EMPHASIS, false);
    options.set(FlexmarkHtmlConverter.EXTRACT_AUTO_LINKS, true);
    options.set(FlexmarkHtmlConverter.WRAP_AUTO_LINKS, false);
    options.set(FlexmarkHtmlConverter.SKIP_LINKS, false);
    options.set(FlexmarkHtmlConverter.SKIP_IMAGES, true); // Пропускаем картинки

    this.converter = FlexmarkHtmlConverter.builder(options).build();
  }

  /**
   * Основной метод конвертации HTML в Markdown для LLM анализа
   * @param html исходный HTML
   * @param maxLength максимальная длина результата (0 = без ограничений)
   * @return чистый Markdown, оптимизированный для анализа LLM
   */
  public String convertToMarkdown(String html, int maxLength) {
    if (html == null || html.trim().isEmpty()) {
      return "";
    }

    try {
      // Предварительная очистка HTML
      String cleanHtml = cleanHtmlForLlm(html);

      // Конвертация в Markdown
      String markdown = converter.convert(cleanHtml);

      // Постобработка для LLM
      markdown = optimizeMarkdownForLlm(markdown);

      // Обрезка при необходимости
      if (maxLength > 0 && markdown.length() > maxLength) {
        markdown = truncateSmartly(markdown, maxLength) + "\n\n*(контент сокращен для анализа)*";
      }

      return markdown;

    } catch (Exception e) {
      log.error("Error converting HTML to Markdown: {}", e.getMessage(), e);
      // Fallback на простое извлечение текста
      return Jsoup.parse(html).text();
    }
  }

  /**
   * Упрощенная версия без ограничения длины
   */
  public String convertToMarkdown(String html) {
    return convertToMarkdown(html, 0);
  }

  /**
   * Специализированная версия для анализа вакансий
   */
  public String convertVacancyToMarkdown(String html, int maxLength) {
    if (html == null || html.trim().isEmpty()) {
      return "";
    }

    try {
      String cleanHtml = cleanVacancyHtml(html);
      String markdown = converter.convert(cleanHtml);
      markdown = structureVacancyMarkdown(markdown);

      if (maxLength > 0 && markdown.length() > maxLength) {
        markdown = truncateSmartly(markdown, maxLength) + "\n\n*(информация о вакансии сокращена)*";
      }

      return markdown;

    } catch (Exception e) {
      log.error("Error converting vacancy HTML: {}", e.getMessage(), e);
      return Jsoup.parse(html).text();
    }
  }

  /**
   * Специализированная версия для JS-страниц и SPA
   */
  public String convertJavaScriptPageToMarkdown(String html, int maxLength) {
    if (html == null || html.trim().isEmpty()) {
      return "";
    }

    try {
      String cleanHtml = cleanJavaScriptPageHtml(html);
      String markdown = converter.convert(cleanHtml);
      markdown = optimizeMarkdownForLlm(markdown);

      if (maxLength > 0 && markdown.length() > maxLength) {
        markdown = truncateSmartly(markdown, maxLength) + "\n\n*(JS-контент сокращен)*";
      }

      return markdown;

    } catch (Exception e) {
      log.error("Error converting JavaScript page HTML: {}", e.getMessage(), e);
      return Jsoup.parse(html).text();
    }
  }

  private String cleanHtmlForLlm(String html) {
    Document doc = Jsoup.parse(html);

    // Удаляем ненужные элементы
    doc.select("script, style, noscript, meta, link").remove();
    doc.select("header, nav, footer, aside").remove();
    doc.select(".advertisement, .ads, .social-share, .popup, .modal").remove();
    doc.select("[class*=cookie], [class*=banner], [class*=overlay]").remove();
    doc.select("iframe, embed, object, video, audio").remove();

    return doc.html();
  }

  private String cleanVacancyHtml(String html) {
    Document doc = Jsoup.parse(html);

    // Базовая очистка
    doc.select("script, style, noscript, meta, link").remove();
    doc.select("header, nav, footer, aside, .sidebar").remove();
    doc.select(".advertisement, .ads, .social, .share").remove();

    // Сохраняем важные для вакансии элементы
    doc.select(".salary, .location, .company-info, .requirements, .responsibilities, .conditions").addClass("important");

    return doc.html();
  }

  private String cleanJavaScriptPageHtml(String html) {
    Document doc = Jsoup.parse(html);

    // Агрессивная очистка для SPA
    doc.select("script, style, noscript, meta, link").remove();
    doc.select("header, nav, footer, aside, .sidebar, .menu").remove();
    doc.select(".advertisement, .ads, .social, .share, .popup, .modal, .overlay").remove();
    doc.select("[class*=cookie], [class*=banner], [class*=notification]").remove();
    doc.select(".loading, .spinner, .placeholder, .skeleton").remove();
    doc.select("[style*=display:none], [style*=visibility:hidden]").remove();

    return doc.html();
  }

  private String optimizeMarkdownForLlm(String markdown) {
    if (markdown == null) return "";

    // Убираем избыточные пустые строки
    markdown = markdown.replaceAll("\n{4,}", "\n\n");

    // Ограничиваем глубину заголовков для лучшего понимания LLM
    markdown = markdown.replaceAll("#{4,}", "###");

    // Убираем пустые элементы списков
    markdown = markdown.replaceAll("\\n\\s*[-*+]\\s*\\n", "\n");

    // Нормализуем избыточное форматирование
    markdown = markdown.replaceAll("\\*{3,}", "**");
    markdown = markdown.replaceAll("_{3,}", "__");

    // Убираем лишние пробелы в начале и конце строк
    StringBuilder cleaned = new StringBuilder();
    for (String line : markdown.split("\n")) {
      cleaned.append(line.trim()).append("\n");
    }

    return cleaned.toString().trim();
  }

  private String structureVacancyMarkdown(String markdown) {
    if (markdown == null) return "";

    // Автоматически структурируем контент вакансии
    markdown = markdown.replaceAll("(?i)(требования|requirements)\\s*:?", "\n## Требования\n");
    markdown = markdown.replaceAll("(?i)(обязанности|responsibilities|duties)\\s*:?", "\n## Обязанности\n");
    markdown = markdown.replaceAll("(?i)(условия|benefits|что мы предлагаем)\\s*:?", "\n## Условия работы\n");
    markdown = markdown.replaceAll("(?i)(о компании|about company|компания)\\s*:?", "\n## О компании\n");
    markdown = markdown.replaceAll("(?i)(зарплата|salary|оплата)\\s*:?", "\n## Зарплата\n");
    markdown = markdown.replaceAll("(?i)(контакты|contacts|связаться)\\s*:?", "\n## Контакты\n");

    return optimizeMarkdownForLlm(markdown);
  }

  private String truncateSmartly(String text, int maxLength) {
    if (text.length() <= maxLength) {
      return text;
    }

    // Пытаемся обрезать по концу раздела (## заголовок)
    int lastSection = text.lastIndexOf("\n## ", maxLength - 50);
    if (lastSection > maxLength * 0.6) {
      return text.substring(0, lastSection);
    }

    // Пытаемся обрезать по концу параграфа
    int lastParagraph = text.lastIndexOf("\n\n", maxLength);
    if (lastParagraph > maxLength * 0.7) {
      return text.substring(0, lastParagraph);
    }

    // Пытаемся обрезать по концу предложения
    int lastSentence = text.lastIndexOf(". ", maxLength);
    if (lastSentence > maxLength * 0.8) {
      return text.substring(0, lastSentence + 1);
    }

    // В крайнем случае обрезаем по словам
    int lastSpace = text.lastIndexOf(' ', maxLength);
    return lastSpace > 0 ? text.substring(0, lastSpace) : text.substring(0, maxLength);
  }
}