package ru.mindils.jb2.app.service.analysis.prompt;

import org.springframework.stereotype.Component;
import ru.mindils.jb2.app.entity.Vacancy;
import ru.mindils.jb2.app.entity.VacancyLlmAnalysisType;
import ru.mindils.jb2.app.util.HtmlToMarkdownConverter;

@Component
public class BenefitsPromptGenerator implements PromptGenerator {

  private final HtmlToMarkdownConverter htmlConverter;

  public BenefitsPromptGenerator(HtmlToMarkdownConverter htmlConverter) {
    this.htmlConverter = htmlConverter;
  }

  @Override
  public VacancyLlmAnalysisType getSupportedType() {
    return VacancyLlmAnalysisType.BENEFITS;
  }

  @Override
  public String generatePrompt(Vacancy vacancy) {
    return """
        Проанализируй IT-вакансию. Верни JSON про льготы и бенефиты.
        
        ДАННЫЕ:
        Название: {name}
        Описание: {description}
        Описание(Бренд): {brandedDescription}
        Компания: {employer}
        Компания(Бренд): {employerBranded}
        
        ПРАВИЛО: Ставь true ТОЛЬКО если ЯВНО написано. Если не упомянуто - пиши false.
        
        === ЧТО ИЩЕМ ===
        
        1. health_insurance (медицина):
        • true = ЯВНО: "ДМС", "медицинское страхование", "медстраховка", "стоматология"
        • false = не упомянуто
        
        2. extended_vacation (расширенный отпуск):
        • true = ЯВНО: "28+ дней отпуска", "30 дней отпуска", "дополнительные выходные", "день рождения выходной"
        • false = не упомянуто
        
        3. wellness (спорт/здоровье):
        • true = ЯВНО: "компенсация фитнеса", "спортзал", "бассейн", "массаж", "SPA", "психолог"
        • false = не упомянуто
        
        4. remote_compensation (компенсация удаленки):
        • true = ЯВНО: "компенсация коворкинга", "оплата интернета", "компенсация связи"
        • false = не упомянуто
        
        5. education (внешнее обучение):
        • true = ЯВНО: "оплата курсов", "компенсация обучения", "сертификации", "Udemy", "Coursera", "книги"
        • false = не упомянуто
        
        6. conferences (конференции):
        • true = ЯВНО: "оплата конференций", "митапы", "воркшопы", "IT-события"
        • false = не упомянуто
        
        7. internal_training (внутреннее обучение):
        • true = ЯВНО: "корпоративные курсы", "менторство", "наставничество", "tech talks"
        • false = не упомянуто
        
        8. paid_sick_leave (оплачиваемые больничные):
        • true = ЯВНО: "100% больничный", "оплата с первого дня", "больничные сверх"
        • false = не упомянуто
        
        ❌ НЕ считай льготами:
        • "Дружный коллектив", "интересные задачи"
        • Зарплата, бонусы, опционы
        • Оборудование
        
        ❌ НЕ выдумывай:
        • Если не написано про ДМС → false
        • Если не написано про обучение → false
        • Только ЯВНОЕ упоминание → true
        
        === ОТВЕТ (только JSON) ===
        {
          "health_insurance": true|false,
          "extended_vacation": true|false,
          "wellness": true|false,
          "remote_compensation": true|false,
          "education": true|false,
          "conferences": true|false,
          "internal_training": true|false,
          "paid_sick_leave": true|false
        }
        """
        .replace("{name}", valueOrEmpty(vacancy.getName()))
        .replace("{description}", htmlConverter.convertToMarkdown(valueOrEmpty(vacancy.getDescription())))
        .replace("{brandedDescription}", htmlConverter.convertToMarkdown(valueOrEmpty(vacancy.getBrandedDescription())))
        .replace("{employer}", htmlConverter.convertToMarkdown(vacancy.getEmployer().getDescription()))
        .replace("{employerBranded}", htmlConverter.convertToMarkdown(vacancy.getEmployer().getBrandedDescription()));
  }

  private String valueOrEmpty(String value) {
    return value != null ? value : "";
  }
}