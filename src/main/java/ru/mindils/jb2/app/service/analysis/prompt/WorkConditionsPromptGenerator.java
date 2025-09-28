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
        Проанализируй описание IT-вакансии на РУССКОМ языке и ОПРЕДЕЛИ УСЛОВИЯ РАБОТЫ и ТРЕБОВАНИЯ К РЕЛОКАЦИИ.
        Приоритизируй русские формулировки и синонимы (учитывай склонения и варианты: «удалёнка/удаленно», «релокация/переезд», «РФ/Россия»).\s
        
        Верни результат ТОЛЬКО в формате JSON (строгий JSON, без комментариев/лишнего текста):
        {
          "workFormat": "remote_global|remote_rf|hybrid_flexible|hybrid_regular|hybrid_frequent|office_only",
          "relocationRequired": "none|optional|required",
          "relocationDestination": "none|within_russia|outside_russia|specific|unspecified",
        }
        
        ОПИСАНИЕ ПОЛЕЙ И ПРАВИЛА:
        
        1) workFormat
           • remote_global — удалённо без гео-ограничений («из любой точки мира/страны», worldwide). можно работать удалённо, находясь за пределами РФ (формулировки «удалённо worldwide», «можно из любой страны», «из стран ЕС/ЕАЭС/UK/US и т.п.»).
           • remote_rf — удалённо только из РФ. удалённая работа разрешена только из РФ/нельзя работать из-за рубежа («только из РФ/РФ-резидент», «нельзя работать из-за границы»).
           • hybrid_flexible — редкие визиты в офис (≈ до 4 дней в месяц, «по необходимости»).
           • hybrid_regular — 1–2 дня в неделю в офисе.
           • hybrid_frequent — 3+ дней в неделю в офисе.
           • office_only — 100% офис.
           Если указано «N дней в офисе в неделю» — маппируй по шкале выше.
        
        2) relocationRequired
           • required — явно «релокация обязательна/переезд обязателен».
           • optional — «возможна/желательна релокация», но не обязательна.
           • none — «без релокации/можно работать из текущей локации» или сигналов нет.
        
        3) relocationDestination
           • none — когда relocationRequired="none".
           • within_russia — целевой город/регион в РФ.
           • outside_russia — за пределами РФ, без конкретной страны/города («в Европу/за рубеж»).
           • specific — названы конкретные страна/город за пределами РФ («Ереван», «Тбилиси», «Алматы», «Берлин»).
           • unspecified — релокация есть, направление не уточнено.
        
        
        
        СИГНАЛЫ (русские; англ. вторично):
        - Удалёнка worldwide: «удалённо из любой точки мира/страны», «full remote worldwide».
        - Удалёнка с ограничениями/только РФ: «можно работать только из РФ/ЕАЭС/ЕС/СНГ», «допустимые страны: …», «нужен часовой пояс …».
        - Обязательная релокация за рубеж: «релокация обязательна в [страна/город]», «обязателен переезд за границу».
        - Опциональная релокация за рубеж: «возможна релокация в [страна/город]».
        - Релокация внутри РФ: «релокация в Москву/СПб/Казань/…».
        
        ПРИОРИТЕТЫ И КОНФЛИКТЫ:
        - Явная обязательная релокация перекрывает заявления про удалёнку: при «обязателен переезд» выбирай workFormat из hybrid_* или office_only по тексту.
        - Если указаны альтернативы «РФ» И «зарубеж»: поставь relocationRequired="optional"; relocationOutOfRussia="optional"; relocationDestination="outside_russia" или "specific" при наличии конкретики.
        
        ДАННЫЕ ДЛЯ АНАЛИЗА:
        Название: {name}
        Описание: {description}
        Описание(Бренд): {descriptionBranded}
        Ключевые навыки: {skills}
        Компания: {employer}
        Компания (Бренд): {employerBranded}
        Компания индустрия: {employerIndustries}
        Зарплата: {salary}
        Город: {city}
        Опыт: {experience}
        График: {schedule}
        Занятость: {employment}
        Формат работы: {workFormat}
        """
        .replace("{name}", valueOrEmpty(vacancy.getName()))
        .replace("{description}", truncateText(valueOrEmpty(vacancy.getDescription()), 2500))
        .replace("{descriptionBranded}", htmlConverter.convertToMarkdown(valueOrEmpty(vacancy.getBrandedDescription())))
        .replace("{skills}", valueOrEmpty(vacancy.getKeySkillsStr()))
        .replace("{employer}", htmlConverter.convertToMarkdown(vacancy.getEmployer().getDescription()))
        .replace("{employerBranded}", htmlConverter.convertToMarkdown(vacancy.getEmployer().getBrandedDescription()))
        .replace("{employerIndustries}", valueOrEmpty(vacancy.getEmployer().getIndustriesStr()))
        .replace("{salary}", valueOrEmpty(vacancy.getSalaryStr()))
        .replace("{city}", valueOrEmpty(vacancy.getCity()))
        .replace("{experience}", getJsonFieldName(vacancy.getExperience()))
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