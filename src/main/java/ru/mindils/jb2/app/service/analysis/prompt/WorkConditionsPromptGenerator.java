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
        Проанализируй описание IT-вакансии и определи условия работы и требования к релокации.
        Верни результат ТОЛЬКО в формате JSON без дополнительного текста.
        
        Описание вакансии:
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
        
        КРИТЕРИИ АНАЛИЗА:
        
        1. ФОРМАТ РАБОТЫ (workFormat):
        ✅ "remote_global" - удаленка из любой точки мира, without geographical restrictions
        ✅ "remote_restricted" - удаленка из определенных стран/регионов
        ✅ "hybrid_flexible" - гибрид ≤4 дня в месяц в офисе, mostly remote
        ✅ "hybrid_regular" - гибрид 1-2 дня в неделю в офисе
        ✅ "hybrid_frequent" - гибрид 3+ дня в неделю в офисе
        ✅ "office_only" - 100% работа в офисе
        
        2. ТРЕБОВАНИЯ К РЕЛОКАЦИИ (relocationRequired):
        ✅ "none" - не требуется, можно работать из РФ/текущего местоположения
        ✅ "assisted" - помогают с релокацией, visa sponsorship
        ✅ "required_no_help" - требуется переезд, но без помощи компании
        ✅ "mandatory_specific" - обязательная релокация в конкретную страну/город
        
        3. ГЕОГРАФИЧЕСКИЕ ОГРАНИЧЕНИЯ (geoRestrictions):
        ✅ "none" - нет географических ограничений
        ✅ "timezone" - ограничения по часовым поясам
        ✅ "country_list" - работа из определенного списка стран
        ✅ "region_specific" - только из определенного региона (EU, US, etc.)
        
        ❌ НЕ УЧИТЫВАТЬ:
        - Командировки и business trips
        - Временные выезды в офис
        - Корпоративные мероприятия
        - Onboarding в офисе
        
        Формат ответа (строгий JSON):
        {
          "workFormat": "remote_global|remote_restricted|hybrid_flexible|hybrid_regular|hybrid_frequent|office_only",
          "relocationRequired": "none|assisted|required_no_help|mandatory_specific",
          "geoRestrictions": "none|timezone|country_list|region_specific"
        }
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