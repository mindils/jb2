package ru.mindils.jb2.app.util;

import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Конвертирует HTML в Markdown с нормализацией кавычек:
 *  «…», „…“, &laquo;…&raquo;, <<…>> → "…"
 */
@Component
public class HtmlToMarkdownConverter {

  private static final Logger log = LoggerFactory.getLogger(HtmlToMarkdownConverter.class);

  // Превращаем пары << ... >> в кавычки, НО не трогаем AsciiDoc <<id,text>> (внутри нет запятых)
  private static final Pattern ANGLE_QUOTED_TEXT =
      Pattern.compile("<<\\s*([^<>,\\n]{1,200}?)\\s*>>");

  private final FlexmarkHtmlConverter converter;

  public HtmlToMarkdownConverter() {
    MutableDataSet options = new MutableDataSet();
    this.converter = FlexmarkHtmlConverter.builder(options).build();
  }

  /**
   * Конвертирует HTML в Markdown.
   * @param html исходный HTML
   * @param maxLength максимальная длина результата (0 = без ограничений)
   * @return чистый Markdown
   */
  public String convertToMarkdown(String html, int maxLength) {
    if (html == null || html.trim().isEmpty()) return "";

    try {
      // 1) Лёгкая чистка HTML
      String cleanHtml = cleanHtml(html);

      // 2) Пред-нормализация кавычек прямо в HTML (и распаковка сущностей)
      cleanHtml = normalizeQuotesInHtml(cleanHtml);

      // 3) Конвертация HTML → Markdown
      String markdown = converter.convert(cleanHtml);

      // 4) На всякий случай: распакуем оставшиеся HTML-сущности в уже полученном Markdown
      markdown = Parser.unescapeEntities(markdown, false);

      // 5) Превращаем любые <<...>> в обычные кавычки (учитывает markdown-окружение)
      markdown = ANGLE_QUOTED_TEXT.matcher(markdown).replaceAll("\"$1\"");

      // 6) Минимальная доводка Markdown (пустые строки/заголовки/trim)
      markdown = optimizeMarkdown(markdown);

      // 7) Обрезка при необходимости
      if (maxLength > 0 && markdown.length() > maxLength) {
        markdown = truncateSmartly(markdown, maxLength) + "\n\n*(контент сокращен)*";
      }

      return markdown;

    } catch (Exception e) {
      log.error("Error converting HTML to Markdown: {}", e.getMessage(), e);
      // Фоллбэк — просто вытащим текст
      return Jsoup.parse(html).text();
    }
  }

  /** Упрощённая версия без ограничения длины. */
  public String convertToMarkdown(String html) {
    return convertToMarkdown(html, 0);
  }

  /** Базовая очистка HTML от мусора. */
  private String cleanHtml(String html) {
    Document doc = Jsoup.parse(html);

    // Удаляем ненужные элементы
    doc.select("script, style, noscript, meta, link").remove();
    doc.select("header, nav, footer, aside").remove();
    doc.select(".advertisement, .ads, .social-share, .popup, .modal").remove();
    doc.select("[class*=cookie], [class*=banner], [class*=overlay]").remove();
    doc.select("iframe, embed, object, video, audio").remove();

    // Изображения обычно не нужны для Markdown-резюме/вакансий
    doc.select("img").remove();

    return doc.html();
  }

  /**
   * Нормализуем кавычки прямо в HTML:
   *  &laquo;&raquo;, «», „“, “”, ‟ → обычные ASCII двойные кавычки ".
   *  Плюс распаковываем HTML-сущности (&amp;quot; и пр.).
   */
  private String normalizeQuotesInHtml(String html) {
    String s = Parser.unescapeEntities(html, false);

    // Все разновидности типографских кавычек → "
    return s.replace('«', '"')
        .replace('»', '"')
        .replace('\u00AB', '"')  // «
        .replace('\u00BB', '"')  // »
        .replace('\u201C', '"')  // “
        .replace('\u201D', '"')  // ”
        .replace('\u201E', '"')  // „
        .replace('\u201F', '"'); // ‟
  }

  /** Минимальная нормализация Markdown (без вмешательства в кавычки). */
  private String optimizeMarkdown(String s) {
    if (s == null || s.isEmpty()) return "";

    // Свести заголовки глубже H3 к H3
    s = s.replaceAll("(?m)^#{4,}\\s*", "### ");

    // Не больше одной пустой строки подряд
    s = s.replaceAll("\\n{3,}", "\n\n");

    // Trim построчно
    StringBuilder out = new StringBuilder(s.length());
    for (String line : s.split("\\R")) {
      out.append(line.trim()).append('\n');
    }
    return out.toString().trim();
  }

  /** Аккуратная обрезка с попыткой резать по параграфу/предложению/слову. */
  private String truncateSmartly(String text, int maxLength) {
    if (text.length() <= maxLength) return text;

    int lastParagraph = text.lastIndexOf("\n\n", maxLength);
    if (lastParagraph >= Math.round(maxLength * 0.7)) {
      return text.substring(0, lastParagraph);
    }

    int lastSentence = text.lastIndexOf(". ", maxLength);
    if (lastSentence >= Math.round(maxLength * 0.8)) {
      return text.substring(0, lastSentence + 1);
    }

    int lastSpace = text.lastIndexOf(' ', maxLength);
    return lastSpace > 0 ? text.substring(0, lastSpace) : text.substring(0, maxLength);
  }
}
