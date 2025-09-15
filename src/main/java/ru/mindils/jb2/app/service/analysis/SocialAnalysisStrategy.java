package ru.mindils.jb2.app.service.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import ru.mindils.jb2.app.entity.AnalysisType;
import ru.mindils.jb2.app.entity.Vacancy;
import ru.mindils.jb2.app.entity.VacancyAnalysis;

@Component
public class SocialAnalysisStrategy implements AnalysisStrategy {

  private static final String SOCIAL_ANALYSIS_PROMPT = """
      Analyze the IT job posting (in Russian) and extract work format and project domain information. Return ONLY flat JSON without additional text.
      
      Job posting:
      Title: {name}
      Description: {description}
      Key skills: {skills}
      
      Analysis criteria:
      
      1. WORK MODE (work_mode) - single value:
         • "remote" - полностью удаленная работа, remote work, дистанционно, из дома
         • "office" - только офис, on-site only, без удаленки
         • "hybrid" - гибрид, hybrid, совмещение офиса и удаленки (без указания конкретных дней)
         • "hybrid_2_3" - гибрид 2 дня офис / 3 дня дома
         • "hybrid_3_2" - гибрид 3 дня офис / 2 дня дома
         • "hybrid_4_1" - гибрид 4 дня офис / 1 день дома
         • "hybrid_flexible" - гибрид с гибким графиком посещения офиса
         • "flexible" - полностью гибкий график (сам выбираешь когда офис/дом)
         • "unknown" - формат не указан или неясен
      
      2. PROJECT DOMAINS (domains) - pipe-separated string of applicable domains:
         • "healthcare" - медицина, здравоохранение, DICOM, медицинские системы, телемедицина, фарма
         • "education" - образование, EdTech, обучение, e-learning, университеты
         • "ecology" - экология, окружающая среда, зеленые технологии
         • "energy" - энергетика, электроэнергия, ЖКХ, utilities
         • "government" - госсектор, госуслуги, муниципальные услуги
         • "social" - социальные услуги, пенсии, пособия, социальная защита
         • "transport" - транспорт, логистика для населения, общественный транспорт
         • "safety" - безопасность, МЧС, пожарная безопасность
         • "employment" - трудоустройство, поиск работы, HR для населения
         • "insurance" - страхование (медицинское, социальное, автострахование)
         • "finance" - финансовые услуги для населения, банки, пенсионные фонды
         • "agriculture" - сельское хозяйство, продовольственная безопасность
         • "science" - научные исследования, R&D
         • "humanitarian" - гуманитарная помощь, благотворительность, НКО
         • "commercial" - коммерческий проект без социальной значимости
         • "unknown" - направление не определено
      
      3. SOCIALLY SIGNIFICANT (socially_significant) - boolean:
         • true - если проект имеет социальную значимость (любой домен кроме "commercial" и "unknown")
         • false - если проект чисто коммерческий или неизвестен
      
      Analysis rules:
      1. work_mode: choose ONE most appropriate value based on job description
      2. domains: can be multiple, separated by "|" (e.g., "healthcare|insurance")
      3. If no domains identified, use "unknown"
      4. If project is clearly commercial without social impact, use "commercial"
      5. Order domains by relevance if multiple apply
      6. Set socially_significant based on whether domains include social value
      7. Case-insensitive matching for Russian and English terms
      8. Analyze actual project description, not just company claims
      
      Response format (strict flat JSON):
      {
        "work_mode": string,
        "domains": string,
        "socially_significant": boolean
      }
      """;

  @Override
  public AnalysisType getAnalysisType() {
    return AnalysisType.SOCIAL;
  }

  @Override
  public String getPrompt(Vacancy vacancy) {
    return SOCIAL_ANALYSIS_PROMPT
        .replace("{name}", vacancy.getName())
        .replace("{description}", truncateText(vacancy.getDescription(), 2000))
        .replace("{skills}", vacancy.getKeySkillsStr());
  }

  @Override
  public void updateAnalysis(VacancyAnalysis analysis, JsonNode llmResponse) {
    analysis.setDomains(llmResponse.get("domains").asText());
    analysis.setWorkMode(llmResponse.get("work_mode").asText());
    analysis.setSociallySignificant(llmResponse.get("socially_significant").asText());
  }

  private String truncateText(String text, int maxLength) {
    if (text == null || text.length() <= maxLength) {
      return text;
    }
    return text.substring(0, maxLength) + "...";
  }
}