package ru.mindils.jb2.app.service.analysis.prompt;

import org.springframework.stereotype.Component;
import ru.mindils.jb2.app.entity.Vacancy;
import ru.mindils.jb2.app.entity.VacancyLlmAnalysisType;
import ru.mindils.jb2.app.util.HtmlToMarkdownConverter;

@Component
public class EquipmentPromptGenerator implements PromptGenerator {

  private final HtmlToMarkdownConverter htmlConverter;

  public EquipmentPromptGenerator(HtmlToMarkdownConverter htmlConverter) {
    this.htmlConverter = htmlConverter;
  }

  @Override
  public VacancyLlmAnalysisType getSupportedType() {
    return VacancyLlmAnalysisType.EQUIPMENT;
  }

  @Override
  public String generatePrompt(Vacancy vacancy) {
    return """
        Проанализируй описание IT-вакансии и определи технические аспекты, особенно предоставляемое оборудование.
        Верни результат ТОЛЬКО в формате JSON без дополнительного текста.
        
        Описание вакансии:
        Название: {name}
        Описание: {description}
        Описание(Бренд): {descriptionBranded}
        Ключевые навыки: {skills}
        Компания: {employer}
        Компания (Бренд): {employerBranded}
        
        КРИТЕРИИ АНАЛИЗА ОБОРУДОВАНИЯ:
        
        ✅ ИЩИ В ТЕКСТЕ:
        - MacBook Pro, Mac, Apple ноутбук → "macbook_pro"
        - Windows ноутбук, ПК, рабочая станция → "windows_laptop"
        - BYOD, "свое оборудование", "собственный ноутбук" → определи уровень компенсации
        - Мониторы, "дополнительный монитор", "два экрана" → "monitors"
        - Периферия, клавиатура, мышь, "оборудование по запросу" → "peripherals"
        - "компенсация", "возмещение расходов" → уровень компенсации
        
        ❌ НЕ ВКЛЮЧАЙ:
        - Программное обеспечение (IDE, ПО)
        - Интернет и связь
        - Офисную мебель
        - Общие упоминания "современное оборудование" без конкретики
        
        УРОВНИ КОМПЕНСАЦИИ BYOD:
        - "full" = полная компенсация, возмещение 100%
        - "partial" = частичная компенсация, доплата
        - "none" = без компенсации
        
        Формат ответа (строгий JSON):
        {
          "equipmentType": "macbook_pro|windows_laptop|byod|not_specified",
          "equipmentProvided": boolean,
          "byodCompensation": "full|partial|none|not_applicable",
          "additionalEquipment": "monitors|peripherals|monitors_peripherals|none",
          "equipmentMentioned": boolean
        }
        """
        .replace("{name}", valueOrEmpty(vacancy.getName()))
        .replace("{description}", htmlConverter.convertToMarkdown(valueOrEmpty(vacancy.getDescription())))
        .replace("{descriptionBranded}", htmlConverter.convertToMarkdown(valueOrEmpty(vacancy.getBrandedDescription())))
        .replace("{skills}", valueOrEmpty(vacancy.getKeySkillsStr()))
        .replace("{employer}", htmlConverter.convertToMarkdown(vacancy.getEmployer().getDescription()))
        .replace("{employerBranded}", htmlConverter.convertToMarkdown(vacancy.getEmployer().getBrandedDescription()));
  }

  private String valueOrEmpty(String value) {
    return value != null ? value : "";
  }
}
