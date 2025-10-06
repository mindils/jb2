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
        Проанализируй IT-вакансию. Верни JSON про оборудование.
        
        ДАННЫЕ:
        Название: {name}
        Описание: {description}
        Описание(Бренд): {descriptionBranded}
        Компания: {employer}
        Компания(Бренд): {employerBranded}
        
        ПРАВИЛО: Отвечай ТОЛЬКО если ЯВНО написано. Если нет информации - пиши "none".
        
        === ЧТО ИЩЕМ ===
        
        1. byod_allowed (можно ли работать на своём ноутбуке):
        • "yes" = ЯВНО написано: "можно на своём", "BYOD", "свой ноутбук", "bring your own device"
        • "no" = ЯВНО написано: "только наша техника", "BYOD запрещён"
        • "none" = ничего не написано
        
        2. macos_mentioned (упоминается ли macOS/Mac):
        • "provided" = ЯВНО написано: "выдаём MacBook", "предоставим Mac"
        • "allowed" = ЯВНО написано: "можно работать на Mac", "можно свой MacBook"
        • "both" = и выдают, и можно свой
        • "none" = нет упоминаний Mac/macOS
        
        3. equipment_compensation (компенсируют ли покупку техники):
        • "full" = ЯВНО написано: "100% компенсация", "полное возмещение", "оплатим технику"
        • "partial" = ЯВНО написано: "частичная компенсация", "доплата", "до N рублей"
        • "none" = нет компенсации или не упоминается
        
        === СИГНАЛЬНЫЕ ФРАЗЫ ===
        
        BYOD:
        • "можно на своём", "свой ноутбук", "BYOD", "bring your own", "личное оборудование"
        
        Mac:
        • "MacBook", "Mac", "macOS", "Apple ноутбук"
        
        Компенсация:
        • "компенсация", "возмещение", "оплатим", "стипендия", "allowance", "stipend"
        
        ❌ НЕ учитывай:
        • Программы, софт, лицензии
        • Интернет, связь
        • Мониторы, клавиатуры, мыши
        • Мебель
        
        ❌ НЕ выдумывай:
        • Если не написано про BYOD → "none"
        • Если не написано про Mac → "none"
        • Если не написано про компенсацию → "none"
        
        === ОТВЕТ (только JSON) ===
        {
          "byod_allowed": "yes|no|none",
          "macos_mentioned": "provided|allowed|both|none",
          "equipment_compensation": "full|partial|none"
        }
        """
        .replace("{name}", valueOrEmpty(vacancy.getName()))
        .replace("{description}", htmlConverter.convertToMarkdown(valueOrEmpty(vacancy.getDescription())))
        .replace("{descriptionBranded}", htmlConverter.convertToMarkdown(valueOrEmpty(vacancy.getBrandedDescription())))
        .replace("{employer}", htmlConverter.convertToMarkdown(vacancy.getEmployer().getDescription()))
        .replace("{employerBranded}", htmlConverter.convertToMarkdown(vacancy.getEmployer().getBrandedDescription()));
  }

  private String valueOrEmpty(String value) {
    return value != null ? value : "";
  }
}