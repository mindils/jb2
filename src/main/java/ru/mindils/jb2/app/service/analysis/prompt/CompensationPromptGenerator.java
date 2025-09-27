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
        Проанализируй описание IT-вакансии и определи параметры компенсации и зарплаты.
        Верни результат ТОЛЬКО в формате JSON без дополнительного текста.
        
        Описание вакансии:
        Название: {name}
        Описание: {description}
        Описание(Бренд): {descriptionBranded}
        Ключевые навыки: {skills}
        Зарплата: {salary}
        Компания: {employer}
        Компания (Бренд): {employerBranded}
        
        КРИТЕРИИ АНАЛИЗА:
        
        1. УКАЗАНА ЛИ ЗАРПЛАТА (salarySpecified):
        ✅ true - если есть конкретные цифры, диапазоны, или четкие указания
        ✅ false - если зарплата не указана, "по договоренности", "конкурентная"
        
        2. ДИАПАЗОН БАЗОВОЙ ЗАРПЛАТЫ (salaryRange):
        ✅ "high_400plus" - базовая зарплата ≥400к рублей в месяц
        ✅ "upper_350_400" - базовая зарплата 350-400к рублей в месяц  
        ✅ "middle_300_350" - базовая зарплата 300-350к рублей в месяц
        ✅ "lower_250_300" - базовая зарплата 250-300к рублей в месяц
        ✅ "below_250" - базовая зарплата менее 250к рублей в месяц
        ✅ "not_specified" - зарплата не указана
        
        3. БЕЛАЯ ЗАРПЛАТА (salaryWhite):
        ✅ true - официальное трудоустройство, белая зарплата, ТК РФ
        ✅ false - серая схема, самозанятость без ТК, договор ГПХ
        
        4. ПРЕМИИ И БОНУСЫ (bonusesAvailable):
        ✅ true - упоминаются премии, бонусы, 13-я зарплата, KPI выплаты
        ✅ false - только фиксированная зарплата, нет упоминаний бонусов
        
        5. АКЦИИ И ОПЦИОНЫ (equityOffered):
        ✅ true - stock options, RSU, акции компании, equity compensation
        ✅ false - нет упоминаний акций или опционов
        
        ❌ НЕ УЧИТЫВАТЬ как зарплату:
        - Социальный пакет и льготы
        - ДМС и страховки  
        - Корпоративное обучение
        - Компенсации за оборудование
        - Питание и развлечения
        
        ❌ НЕ СЧИТАТЬ белой зарплатой:
        - Работа как ИП или самозанятый
        - Договоры ГПХ без трудовых гарантий
        - Упоминание "налоги за ваш счет"
        
        Формат ответа (строгий JSON):
        {
          "salarySpecified": boolean,
          "salaryRange": "high_400plus|upper_350_400|middle_300_350|lower_250_300|below_250|not_specified",
          "salaryWhite": boolean,
          "bonusesAvailable": boolean,
          "equityOffered": boolean
        }
        """
        .replace("{name}", valueOrEmpty(vacancy.getName()))
        .replace("{description}", htmlConverter.convertToMarkdown(valueOrEmpty(vacancy.getDescription())))
        .replace("{descriptionBranded}", htmlConverter.convertToMarkdown(valueOrEmpty(vacancy.getBrandedDescription())))
        .replace("{skills}", valueOrEmpty(vacancy.getKeySkillsStr()))
        .replace("{salary}", valueOrEmpty(vacancy.getSalaryStr()))
        .replace("{employer}", htmlConverter.convertToMarkdown(vacancy.getEmployer().getDescription()))
        .replace("{employerBranded}", htmlConverter.convertToMarkdown(vacancy.getEmployer().getBrandedDescription()));
  }

  private String valueOrEmpty(String value) {
    return value != null ? value : "";
  }
}