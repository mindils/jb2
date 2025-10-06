package ru.mindils.jb2.app.service.analysis.prompt;

import org.springframework.stereotype.Component;
import ru.mindils.jb2.app.entity.Vacancy;
import ru.mindils.jb2.app.entity.VacancyLlmAnalysisType;
import ru.mindils.jb2.app.util.HtmlToMarkdownConverter;

@Component
public class IndustryPromptGenerator implements PromptGenerator {
  private final HtmlToMarkdownConverter htmlConverter;

  public IndustryPromptGenerator(HtmlToMarkdownConverter htmlConverter) {
    this.htmlConverter = htmlConverter;
  }

  @Override
  public VacancyLlmAnalysisType getSupportedType() {
    return VacancyLlmAnalysisType.INDUSTRY;
    }

  @Override
  public String generatePrompt(Vacancy vacancy) {
    return """
        Определи ОТРАСЛЬ компании и проекта по описанию IT-вакансии.
        
        ФОРМАТ ОТВЕТА (только JSON, без текста):
        {
          "company_category": "safe|neutral|toxic",
          "project_category": "safe|neutral|toxic",
          "company_direction": "healthcare|education|energy|b2b_tech|fintech|consumer|advertising|harmful|none",
          "project_direction": "healthcare|education|energy|b2b_tech|fintech|consumer|advertising|harmful|none"
        }
        
        КАТЕГОРИИ:
        - safe: полезная/социально значимая (healthcare, education, energy)
        - neutral: нейтральная/коммерческая (b2b_tech, fintech, consumer)
        - toxic: вредная/опасная (advertising, harmful)
        
        НАПРАВЛЕНИЯ И СИГНАЛЫ:
        
        1. healthcare → safe
        СИГНАЛЫ: медицина, здравоохранение, клиника, больница, телемедицина, МИС, ЕГИСЗ, медтех
        
        2. education → safe
        СИГНАЛЫ: образование, школа, вуз, университет, курсы, обучение, эдтех, платформа обучения
        
        3. energy → safe
        СИГНАЛЫ: энергетика, возобновляемая энергия, чистая энергия, солнечная энергия, ветровая энергия, зеленая энергия, ВИЭ, электростанция, энергосети, умные сети, smart grid
        
        4. b2b_tech → neutral
        СИГНАЛЫ: SaaS, CRM, ERP, B2B, для бизнеса, логистика, склад, производство, завод, govtech, госуслуги, mlops
        
        5. fintech → neutral
        СИГНАЛЫ: банк, финтех, платежи, эквайринг, страхование, недвижимость, юридические услуги, KYC, брокер, трейдинг
        ВАЖНО: НЕ путай с микрозаймами - они toxic!
        
        6. consumer → neutral
        СИГНАЛЫ: маркетплейс, e-commerce, интернет-магазин, игры, геймдев, стриминг, развлечения, музыка, кино
        
        7. advertising → toxic
        СИГНАЛЫ: реклама, рекламная платформа, adtech, DSP, SSP, программатик, маркетинг-платформа, CDP, таргетинг, рекламные сети
        
        8. harmful → toxic (ТОКСИЧНЫЕ ИНДУСТРИИ)
        СИГНАЛЫ:
        - МИКРОФИНАНСИРОВАНИЕ (ТОКСИЧНО): МФО, микрофинансовая организация, микрозайм, микрокредит, займ до зарплаты, payday loan, быстрый займ, онлайн займ, краткосрочный кредит
        - ЗНАКОМСТВА: дейтинг, сервис знакомств, приложение для знакомств, dating
        - ВРЕДНЫЕ ВЕЩЕСТВА: табак, табачная продукция, сигареты, вейп, алкоголь, алкогольная продукция, спиртные напитки
        - ОРУЖИЕ: оружие, оборонная промышленность, ВПК, военно-промышленный комплекс
        
        ГДЕ ИСКАТЬ СИГНАЛЫ (порядок важности):
        1. "Индустрия" (employerIndustries) - самый надежный источник
        2. "Компания" (employer) - описание бизнеса компании
        3. "Вакансия" (name) - название позиции может указывать на отрасль
        4. "Описание" (description) - детали проекта и задач
        
        ПРАВИЛА:
        1. Сначала определи компанию по полям: Индустрия → Компания
        2. Потом проект по полям: Вакансия → Описание
        3. Если проект не отличается от компании: project_direction = company_direction, project_category = company_category
        4. Если сигналов нет: direction = "none", category = "neutral"
        5. Игнорируй технологии: Java, Python, React, Kubernetes и т.д.
        6. При нескольких направлениях: выбери ОДНО основное
        7. ВАЖНО: МФО и микрозаймы = harmful (toxic), НЕ fintech!
        
        ДАННЫЕ:
        Вакансия: {name}
        Описание: {description}
        Компания: {employer}
        Индустрия: {employerIndustries}
        """
        .replace("{name}", valueOrEmpty(vacancy.getName()))
        .replace("{description}", htmlConverter.convertToMarkdown(
            valueOrEmpty(vacancy.getDescription()) + " " +
                valueOrEmpty(vacancy.getBrandedDescription())))
        .replace("{employer}", htmlConverter.convertToMarkdown(
            vacancy.getEmployer().getDescription() + " " +
                vacancy.getEmployer().getBrandedDescription()))
        .replace("{employerIndustries}", valueOrEmpty(vacancy.getEmployer().getIndustriesStr()));
  }

  private String valueOrEmpty(String value) {
    return value != null ? value : "";
  }

  private String truncate(String text, int maxLength) {
    if (text == null || text.length() <= maxLength) {
      return text;
    }
    return text.substring(0, maxLength) + "...";
  }
}