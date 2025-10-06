package ru.mindils.jb2.app.service.analysis.prompt;
import org.springframework.stereotype.Component;
import ru.mindils.jb2.app.entity.Vacancy;
import ru.mindils.jb2.app.entity.VacancyLlmAnalysisType;
import ru.mindils.jb2.app.util.HtmlToMarkdownConverter;
@Component
public class WorkConditionsPromptGenerator implements PromptGenerator {
  private final HtmlToMarkdownConverter htmlConverter;
  public WorkConditionsPromptGenerator(HtmlToMarkdownConverter htmlConverter) {
    this.htmlConverter = htmlConverter;
  }
  @Override
  public VacancyLlmAnalysisType getSupportedType() {
    return VacancyLlmAnalysisType.WORK_CONDITIONS;
  }
  @Override
  public String generatePrompt(Vacancy vacancy) {
    return """
        Проанализируй IT-вакансию и определи условия работы. Отвечай ТОЛЬКО на основе текста вакансии.
        
        ⚠️ ВАЖНО: Если информации НЕТ в тексте — пиши "none". НЕ придумывай.
        
        Верни JSON:
        {
          "workFormat": "remote|hybrid_flexible|hybrid_regular|hybrid_frequent|office_only|none",
          "canWorkAbroad": "yes|no|none",
          "relocationRequired": "none|optional|required",
          "relocationDestination": "none|within_russia|outside_russia|specific"
        }
        
        === ИНСТРУКЦИЯ ПО ЗАПОЛНЕНИЮ ===
        
        Шаг 1. Определи workFormat (формат работы):
        
        Ищи фразы:
        - "удалённо" / "удаленно" / "remote" / "из дома" → пиши "remote"
        - "офис" / "office" / "в офисе" → пиши "office_only"
        - "гибрид" / "hybrid" / "частично офис" / "N дня в офисе" → смотри ниже
        
        Если УДАЛЁННО:
        → "remote"
        
        Если ГИБРИД (часть времени в офисе):
        - До 1 дня в неделю в офисе → "hybrid_flexible"
        - 1-2 дня в неделю → "hybrid_regular"  
        - 3+ дня в неделю → "hybrid_frequent"
        
        Если ТОЛЬКО ОФИС:
        → "office_only"
        
        Если НЕТ информации о формате:
        → "none"
        
        ---
        
        Шаг 2. Определи canWorkAbroad (можно ли работать из-за границы):
        
        Ищи фразы про ГДЕ можно работать:
        
        МОЖНО за границей → "yes":
        - "из любой страны"
        - "из любой точки мира"
        - "worldwide"
        - "из Европы" / "из стран ЕС"
        - "из-за рубежа"
        - "международная команда"
        
        НЕЛЬЗЯ за границей → "no":
        - "только из России"
        - "только РФ"
        - "нельзя из-за границы"
        - "резидент РФ"
        
        НЕТ информации → "none":
        - Про географию вообще не написано
        
        ---
        
        Шаг 3. Определи relocationRequired (нужен ли переезд):
        
        ОБЯЗАТЕЛЬНО → "required":
        - "релокация обязательна"
        - "переезд обязателен"
        - "требуется переезд"
        
        НЕ ОБЯЗАТЕЛЬНО → "optional":
        - "возможна релокация"
        - "поможем с переездом"
        - "готовы к релокации"
        
        НЕТ → "none":
        - Про переезд не написано ИЛИ
        - "без релокации" ИЛИ
        - "переезд не требуется"
        
        ---
        
        Шаг 4. Определи relocationDestination (куда переезд):
        
        Это поле заполняй ТОЛЬКО если relocationRequired = "optional" или "required"
        
        ВНУТРИ РОССИИ → "within_russia":
        - "в Москву" / "в СПб" / "в Казань" и т.п.
        
        ЗА ГРАНИЦУ БЕЗ КОНКРЕТИКИ → "outside_russia":
        - "за рубеж"
        - "в Европу"
        - "за границу"
        
        КОНКРЕТНАЯ СТРАНА/ГОРОД → "specific":
        - "в Ереван"
        - "в Тбилиси"
        - "в Берлин"
        - "в Казахстан"
        
        Если relocationRequired = "none" → "none"
        
        === ПРИМЕРЫ ===
        
        Пример 1: "Удалённо из любой точки мира"
        {
          "workFormat": "remote",
          "canWorkAbroad": "yes",
          "relocationRequired": "none",
          "relocationDestination": "none"
        }
        
        Пример 2: "Удалённо, только из РФ"
        {
          "workFormat": "remote",
          "canWorkAbroad": "no",
          "relocationRequired": "none",
          "relocationDestination": "none"
        }
        
        Пример 3: "Гибрид 2 дня в офисе, можно из Европы"
        {
          "workFormat": "hybrid_regular",
          "canWorkAbroad": "yes",
          "relocationRequired": "none",
          "relocationDestination": "none"
        }
        
        Пример 4: "Возможна релокация в Берлин"
        {
          "workFormat": "none",
          "canWorkAbroad": "none",
          "relocationRequired": "optional",
          "relocationDestination": "specific"
        }
        
        Пример 5: "Офис в Москве, релокация обязательна"
        {
          "workFormat": "office_only",
          "canWorkAbroad": "no",
          "relocationRequired": "required",
          "relocationDestination": "within_russia"
        }
        
        === ДАННЫЕ ВАКАНСИИ ===
        
        Название: {name}
        Город: {city}
        График: {schedule}
        Занятость: {employment}
        Формат работы: {workFormat}
        
        Описание:
        {description}
        
        Описание (брендированное):
        {descriptionBranded}
        
        О компании:
        {employer}
        
        О компании (брендированное):
        {employerBranded}
        
        Индустрия: {employerIndustries}
        """
        .replace("{name}", valueOrEmpty(vacancy.getName()))
        .replace("{description}", htmlConverter.convertToMarkdown(valueOrEmpty(vacancy.getDescription())))
        .replace("{descriptionBranded}", htmlConverter.convertToMarkdown(valueOrEmpty(vacancy.getBrandedDescription())))
        .replace("{employer}", htmlConverter.convertToMarkdown(vacancy.getEmployer().getDescription()))
        .replace("{employerBranded}", htmlConverter.convertToMarkdown(vacancy.getEmployer().getBrandedDescription()))
        .replace("{employerIndustries}", valueOrEmpty(vacancy.getEmployer().getIndustriesStr()))
        .replace("{city}", valueOrEmpty(vacancy.getCity()))
        .replace("{schedule}", getJsonFieldName(vacancy.getSchedule()))
        .replace("{employment}", getJsonFieldName(vacancy.getEmployment()))
        .replace("{workFormat}", valueOrEmpty(vacancy.getWorkFormatStr()));
  }
  private String valueOrEmpty(String value) {
    return value != null ? value : "";
  }
  private String getJsonFieldName(com.fasterxml.jackson.databind.JsonNode node) {
    return node != null ? node.path("name").asText("") : "";
  }
  private String truncateText(String text, int maxLength) {
    if (text == null || text.length() <= maxLength) return text;
    return text.substring(0, maxLength) + "...";
  }
}