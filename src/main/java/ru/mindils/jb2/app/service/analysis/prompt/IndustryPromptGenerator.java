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
        Проанализируй описание IT-вакансии и определи отрасль компании и проекта. 
        Верни результат ТОЛЬКО в формате JSON без дополнительного текста.
        
        Описание вакансии:
        Название: {name}
        Описание: {description}
        Описание(Бренд): {descriptionBranded}
        Ключевые навыки: {skills}
        Компания: {employer}
        Компания (Бренд): {employerBranded}
        Компания индустрия: {employerIndustries}
        
        1. КАТЕГОРИЯ КОМПАНИИ (company_category) - одно значение:
           • "positive" - медицина, образование, экология, наука, космос, социальные проекты, доступность, сельское хозяйство
           • "neutral" - B2B SaaS, госуслуги, логистика, производство, AI инфраструктура, глубокие технологии
           • "problematic" - финансы, страхование, недвижимость, юридические услуги, маркетинг
           • "toxic" - e-commerce, игры, развлечения, реклама, знакомства, микрофинансы, табак/алкоголь, оружие
        
        2. КАТЕГОРИЯ ПРОЕКТА (project_category) - одно значение:
           Если проект описан отдельно - применить ту же логику
           Если не отличается от компании - указать ту же категорию
        
        3. НАПРАВЛЕНИЕ КОМПАНИИ (company_direction) - строка через "|":
           Конкретные отрасли из списка:
           "healthcare|education|ecology|science|space|social_impact|accessibility|agriculture|b2b_saas|govtech|logistics|manufacturing|ai_infrastructure|deeptech|fintech|insurtech|proptech|legaltech|martech|ecommerce|gaming|entertainment|adtech|dating|microcredit|tobacco_alcohol|weapons"
        
        4. НАПРАВЛЕНИЕ ПРОЕКТА (project_direction) - строка через "|":
           Если проект отличается от компании - указать его направления
           Если не отличается - указать пустую строку ""
        
        Формат ответа (строгий JSON):
        {
          "company_category": string,
          "project_category": string,
          "company_direction": string,
          "project_direction": string
        }
        """
        .replace("{name}", valueOrEmpty(vacancy.getName()))
        .replace("{description}", htmlConverter.convertToMarkdown(valueOrEmpty(vacancy.getDescription())))
        .replace("{descriptionBranded}", htmlConverter.convertToMarkdown(valueOrEmpty(vacancy.getBrandedDescription())))
        .replace("{skills}", valueOrEmpty(vacancy.getKeySkillsStr()))
        .replace("{employer}", htmlConverter.convertToMarkdown(vacancy.getEmployer().getDescription()))
        .replace("{employerBranded}", htmlConverter.convertToMarkdown(vacancy.getEmployer().getBrandedDescription()))
        .replace("{employerIndustries}", valueOrEmpty(vacancy.getEmployer().getIndustriesStr()));
  }

  private String valueOrEmpty(String value) {
    return value != null ? value : "";
  }
}