package ru.mindils.jb2.app.service.analysis.prompt;

import org.springframework.stereotype.Component;
import ru.mindils.jb2.app.entity.Vacancy;
import ru.mindils.jb2.app.entity.VacancyLlmAnalysisType;
import ru.mindils.jb2.app.util.HtmlToMarkdownConverter;

@Component
public class CompensationPromptGenerator implements PromptGenerator {

  private final HtmlToMarkdownConverter htmlConverter;

  public CompensationPromptGenerator(HtmlToMarkdownConverter htmlConverter) {
    this.htmlConverter = htmlConverter;
  }

  @Override
  public VacancyLlmAnalysisType getSupportedType() {
    return VacancyLlmAnalysisType.COMPENSATION;
  }

  @Override
  public String generatePrompt(Vacancy vacancy) {
    return """
        Проанализируй IT-вакансию. Верни JSON с информацией о зарплате.
        
        ДАННЫЕ:
        Название: {name}
        Зарплата: {salary}
        Описание: {description}
        Описание(Бренд): {descriptionBranded}
        
        ПРАВИЛО: Отвечай ТОЛЬКО на основе текста. Если информации НЕТ - пиши "none"
        
        === ЧТО АНАЛИЗИРОВАТЬ ===
        
        1. salaryRange (диапазон в месяц):
        • "high_400plus" = от 400к₽ и выше
        • "upper_350_400" = 350-400к₽
        • "middle_300_350" = 300-350к₽
        • "lower_250_300" = 250-300к₽
        • "below_250" = меньше 250к₽
        • "none" = зарплата не указана
        
        2. salaryType (тип оформления):
        • "white" = ЯВНО написано: "ТК РФ", "официальное трудоустройство", "белая зарплата"
        • "gray" = ЯВНО написано: "самозанятость", "ИП", "ГПХ", "налоги за ваш счет", "серая схема"
        • "none" = ничего не написано про тип оформления
        
        3. bonusesAvailable (есть ли бонусы):
        • true = ЯВНО написано: "бонусы", "премии", "KPI", "13-я зарплата"
        • false = ничего не написано про бонусы
        
        4. equityOffered (есть ли опционы/акции):
        • true = ЯВНО написано: "опционы", "stock options", "RSU", "акции компании"
        • false = ничего не написано про опционы
        
        === ВАЖНО ===
        
        ❌ НЕ включай в зарплату:
        • ДМС, страховки
        • обучение, конференции
        • оборудование
        • питание
        
        ❌ НЕ выдумывай:
        • Если не написано про ТК РФ → salaryType = "none"
        • Если не написано про бонусы → bonusesAvailable = false
        • Если зарплаты нет → salaryRange = "none"
        
        === ОТВЕТ (только JSON) ===
        {
          "salaryRange": "high_400plus|upper_350_400|middle_300_350|lower_250_300|below_250|none",
          "salaryType": "white|gray|none",
          "bonusesAvailable": true|false,
          "equityOffered": true|false
        }
        """
        .replace("{name}", valueOrEmpty(vacancy.getName()))
        .replace("{salary}", valueOrEmpty(vacancy.getSalaryStr()))
        .replace("{description}", htmlConverter.convertToMarkdown(valueOrEmpty(vacancy.getDescription())))
        .replace("{descriptionBranded}", htmlConverter.convertToMarkdown(valueOrEmpty(vacancy.getBrandedDescription())));
  }

  private String valueOrEmpty(String value) {
    return value != null ? value : "";
  }
}