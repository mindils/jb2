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
        Проанализируй IT-вакансию. Верни JSON про стоп-факторы.
        
        ДАННЫЕ:
        Название: {name}
        Описание: {description}
        Описание(Бренд): {descriptionBranded}
        Компания: {employer}
        Компания(Бренд): {employerBranded}
        Индустрия: {employerIndustries}
        
        ПРАВИЛО: Ставь true ТОЛЬКО если ЯВНО написано. Если не упомянуто - пиши false.
        
        === ЧТО ИЩЕМ ===
        
        1. toxic_culture (токсичная культура):
        • true = ЯВНО: "переработки обязательны", "работа сверхурочно", "аврал", "горящие проекты постоянно", "микроменеджмент", "строгий контроль каждого действия"
        • false = не упомянуто или нормальные условия
        
        2. banned_domain (запрещенная индустрия):
        • true = ЯВНО про:
          - Gambling: "казино", "ставки", "букмекер", "азартные игры", "betting"
          - Микрозаймы: "МФО", "микрозайм", "займ до зарплаты", "быстрый кредит"
          - Adult: "adult", "18+", "эротический контент"
          - Оружие: "оружие", "оборонная промышленность", "ВПК"
          - Табак/вейп: "табачная продукция", "сигареты", "вейп"
        • false = не упомянуто
        
        ❌ НЕ считай banned_domain:
        • Банки, финтех, страхование (кроме МФО)
        • Рестораны/бары (алкоголь в общепите - нормально)
        • IT-компании с разными клиентами
        
        ❌ НЕ считай toxic_culture:
        • "Интересные задачи", "быстрый темп"
        • "Deadline-ориентированность" (это нормально)
        • "Требования к качеству кода"
        
        ❌ НЕ выдумывай:
        • Если не написано про переработки → false
        • Если не написано про запрещенную сферу → false
        • Только ЯВНОЕ упоминание → true
        
        === ОТВЕТ (только JSON) ===
        {
          "toxic_culture": true|false,
          "banned_domain": true|false
        }
        """
        .replace("{name}", valueOrEmpty(vacancy.getName()))
        .replace("{description}", htmlConverter.convertToMarkdown(valueOrEmpty(vacancy.getDescription())))
        .replace("{descriptionBranded}", htmlConverter.convertToMarkdown(valueOrEmpty(vacancy.getBrandedDescription())))
        .replace("{employer}", htmlConverter.convertToMarkdown(vacancy.getEmployer().getDescription()))
        .replace("{employerBranded}", htmlConverter.convertToMarkdown(vacancy.getEmployer().getBrandedDescription()))
        .replace("{employerIndustries}", valueOrEmpty(vacancy.getEmployer().getIndustriesStr()));
  }

  private String valueOrEmpty(String value) {
    return value != null ? value : "";
  }

  private String getJsonFieldName(com.fasterxml.jackson.databind.JsonNode node) {
    return node != null ? node.path("name").asText("") : "";
  }
}