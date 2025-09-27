package ru.mindils.jb2.app.service.analysis.prompt;

import org.springframework.stereotype.Component;
import ru.mindils.jb2.app.entity.Vacancy;
import ru.mindils.jb2.app.entity.VacancyLlmAnalysisType;
import ru.mindils.jb2.app.util.HtmlToMarkdownConverter;

@Component
public class StopFactorsPromptGenerator implements PromptGenerator {

  private final HtmlToMarkdownConverter htmlConverter;

  public StopFactorsPromptGenerator(HtmlToMarkdownConverter htmlConverter) {
    this.htmlConverter = htmlConverter;
  }

  @Override
  public VacancyLlmAnalysisType getSupportedType() {
    return VacancyLlmAnalysisType.STOP_FACTORS;
  }

  @Override
  public String generatePrompt(Vacancy vacancy) {
    return """
        Проанализируй описание IT-вакансии на наличие критических стоп-факторов.
        Верни результат ТОЛЬКО в формате JSON без дополнительного текста.
        
        Описание вакансии:
        Название: {name}
        Описание: {description}
        Описание(Бренд): {descriptionBranded}
        Ключевые навыки: {skills}
        Зарплата: {salary}
        Компания: {employer}
        Компания (Бренд): {employerBranded}
        Компания индустрия: {employerIndustries}
        Город: {city}
        Опыт: {experience}
        График: {schedule}
        Занятость: {employment}
        Формат работы: {workFormat}
        
        КРИТЕРИИ СТОП-ФАКТОРОВ:
        
        1. СЕРАЯ/ЧЕРНАЯ ЗАРПЛАТА (graySalary):
        ✅ ИЩИТЕ ПРИЗНАКИ:
        - "зарплата в конверте", "часть зарплаты наличными"
        - "зарплата по договоренности без официального оформления"
        - "работа без трудового договора", "самозанятость обязательна"
        - "серая/черная схема", "минимальная официальная зарплата"
        
        2. ТОКСИЧНАЯ КУЛЬТУРА (toxicCulture):
        ✅ ИЩИТЕ ПРИЗНАКИ:
        - "переработки", "готовность работать сверхурочно"
        - "высокие требования к стрессоустойчивости"
        - "жесткие дедлайны", "аврально", "горящие проекты"
        - "микроменеджмент", "строгий контроль"
        - "высокая текучка", "быстрая замена сотрудников"
        
        3. ЗАПРЕЩЕННЫЕ ДОМЕНЫ (bannedDomain):
        ✅ ИЩИТЕ ПРИЗНАКИ:
        - Gambling: "казино", "ставки", "игорный", "букмекер", "азартные игры"
        - Микрокредитование: "микрозайм", "быстрые кредиты", "займ до зарплаты"
        - Оружие: "военная промышленность", "оборонка", "оружие"
        - Adult: "adult", "18+", "эротический", "порно"
        - Алкоголь/табак: продвижение алкоголя/табака (не общепит)
        - Мошенничество: "пирамида", "сетевой маркетинг", подозрительные схемы
        
        ❌ НЕ ВКЛЮЧАЙТЕ:
        - Банки, финтех (кроме микрозаймов)
        - Рестораны/бары (продажа алкоголя в рамках общепита)
        - IT-компании с разными клиентами
        - Общие фразы без конкретики
        
        ВАЖНО: Если хотя бы один фактор = true, то stopFactorFound = true
        
        Формат ответа (строгий JSON):
        {
          "graySalary": boolean,
          "toxicCulture": boolean,
          "bannedDomain": boolean,
          "stopFactorFound": boolean
        }
        """
        .replace("{name}", valueOrEmpty(vacancy.getName()))
        .replace("{description}", htmlConverter.convertToMarkdown(valueOrEmpty(vacancy.getDescription())))
        .replace("{descriptionBranded}", htmlConverter.convertToMarkdown(valueOrEmpty(vacancy.getBrandedDescription())))
        .replace("{skills}", valueOrEmpty(vacancy.getKeySkillsStr()))
        .replace("{salary}", valueOrEmpty(vacancy.getSalaryStr()))
        .replace("{employer}", htmlConverter.convertToMarkdown(vacancy.getEmployer().getDescription()))
        .replace("{employerBranded}", htmlConverter.convertToMarkdown(vacancy.getEmployer().getBrandedDescription()))
        .replace("{employerIndustries}", valueOrEmpty(vacancy.getEmployer().getIndustriesStr()))
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
}
