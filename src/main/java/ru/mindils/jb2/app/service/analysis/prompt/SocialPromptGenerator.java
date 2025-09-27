package ru.mindils.jb2.app.service.analysis.prompt;

import org.springframework.stereotype.Component;
import ru.mindils.jb2.app.entity.Vacancy;
import ru.mindils.jb2.app.entity.VacancyLlmAnalysisType;

@Component
public class SocialPromptGenerator implements PromptGenerator {

  @Override
  public VacancyLlmAnalysisType getSupportedType() {
    return VacancyLlmAnalysisType.SOCIAL;
  }

  @Override
  public String generatePrompt(Vacancy vacancy) {
    return """
        Analyze the IT job posting (in Russian) and extract work format and project domain information. Return ONLY flat JSON without additional text.

        Job posting:
        Title: {name}
        Description: {description}
        Key skills: {skills}
        Salary: {salary}
        City: {city}
        Metro: {metro}
        Experience: {experience}
        Schedule: {schedule}
        Employment: {employment}
        Professional roles: {professionalRoles}
        Work format: {workFormat}
        Premium: {premium}
        Internship: {internship}
        
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
        
        Response format (strict flat JSON):
        {
          "work_mode": string,
          "domains": string,
          "socially_significant": boolean
        }
        """
        .replace("{name}", valueOrEmpty(vacancy.getName()))
        .replace("{description}", truncateText(valueOrEmpty(vacancy.getDescription()), 2000))
        .replace("{skills}", valueOrEmpty(vacancy.getKeySkillsStr()))
        .replace("{salary}", valueOrEmpty(vacancy.getSalaryStr()))
        .replace("{city}", valueOrEmpty(vacancy.getCity()))
        .replace("{metro}", valueOrEmpty(vacancy.getMetro()))
        .replace("{experience}", getJsonFieldName(vacancy.getExperience()))
        .replace("{schedule}", getJsonFieldName(vacancy.getSchedule()))
        .replace("{employment}", getJsonFieldName(vacancy.getEmployment()))
        .replace("{professionalRoles}", valueOrEmpty(vacancy.getProfessionalRolesStr()))
        .replace("{workFormat}", valueOrEmpty(vacancy.getWorkFormatStr()))
        .replace("{premium}", String.valueOf(vacancy.getPremium()))
        .replace("{internship}", String.valueOf(vacancy.getInternship()));
  }

  private String valueOrEmpty(String value) {
    return value != null ? value : "";
  }

  private String getJsonFieldName(com.fasterxml.jackson.databind.JsonNode node) {
    return node != null ? node.path("name").asText("") : "";
  }

  private String truncateText(String text, int maxLength) {
    if (text == null || text.length() <= maxLength) return text;
    return text.substring(0, maxLength) + "...";
  }
}