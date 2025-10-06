package ru.mindils.jb2.app.service.analysis;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Правила оценки вакансий
 * Каждое правило содержит оценку и описание
 * Префиксы соответствуют VacancyLlmAnalysisType
 */
public class ScoringRules {

  @Getter
  @AllArgsConstructor
  public static class Rule {
    private final int score;
    private final String description; // null = не показывать в описании

    // Конструктор для правил без описания (только оценка)
    public Rule(int score) {
      this.score = score;
      this.description = null;
    }
  }

  // ============ JAVA_PRIMARY ============
  public static final Rule JAVA_PRIMARY_JAVA = new Rule(0);
  public static final Rule JAVA_PRIMARY_JMIX = new Rule(150, "Jmix фреймворк");

  // ============ TECHNICAL ============
  // Роли
  public static final Rule TECHNICAL_ROLE_BACKEND = new Rule(50);
  public static final Rule TECHNICAL_ROLE_FULLSTACK = new Rule(40, "Fullstack разработка");
  public static final Rule TECHNICAL_ROLE_DEVOPS = new Rule(30, "DevOps с разработкой");
  public static final Rule TECHNICAL_ROLE_OTHER = new Rule(-100, "Другая роль");
  public static final Rule TECHNICAL_ROLE_NONE = new Rule(0); // роль не указана

  // Уровни
  public static final Rule TECHNICAL_LEVEL_ARCHITECT = new Rule(20);
  public static final Rule TECHNICAL_LEVEL_PRINCIPAL = new Rule(20);
  public static final Rule TECHNICAL_LEVEL_SENIOR = new Rule(20);
  public static final Rule TECHNICAL_LEVEL_LEAD = new Rule(20);
  public static final Rule TECHNICAL_LEVEL_MIDDLE = new Rule(20);
  public static final Rule TECHNICAL_LEVEL_JUNIOR = new Rule(-10);
  public static final Rule TECHNICAL_LEVEL_NONE = new Rule(0); // уровень не указан

  // Стек
  public static final Rule TECHNICAL_STACK_SPRING = new Rule(0);
  public static final Rule TECHNICAL_STACK_MICROSERVICES = new Rule(0);
  public static final Rule TECHNICAL_STACK_DATABASE = new Rule(0);
  public static final Rule TECHNICAL_STACK_PYTHON = new Rule(10, "Python в стеке");
  public static final Rule TECHNICAL_STACK_DEVOPS = new Rule(30, "DevOps технологии");
  public static final Rule TECHNICAL_STACK_FRONTEND = new Rule(30, "Frontend разработка");
  public static final Rule TECHNICAL_STACK_NONE = new Rule(0); // стек не указан

  // AI
  public static final Rule TECHNICAL_AI_ALLOWED = new Rule(30, "AI инструменты разрешены");
  public static final Rule TECHNICAL_AI_PROJECT_OPTIONAL = new Rule(40, "AI проекты опционально");
  public static final Rule TECHNICAL_AI_PROJECT_REQUIRED = new Rule(-50, "AI проекты обязательны");
  public static final Rule TECHNICAL_AI_NONE = new Rule(0); // AI не упоминается

  // ============ COMPENSATION ============
  public static final Rule COMPENSATION_NONE = new Rule(0); // зарплата не указана
  public static final Rule COMPENSATION_HIGH_400 = new Rule(100, "Высокая ЗП (400k+)");
  public static final Rule COMPENSATION_350_400 = new Rule(50, "ЗП 350-400k");
  public static final Rule COMPENSATION_350_400_BONUS = new Rule(60, "ЗП 350-400k + бонусы");
  public static final Rule COMPENSATION_300_350 = new Rule(0, "ЗП 300-350k");
  public static final Rule COMPENSATION_300_350_BONUS = new Rule(20, "ЗП 300-350k + бонусы");
  public static final Rule COMPENSATION_250_300 = new Rule(-100, "ЗП 250-300k");
  public static final Rule COMPENSATION_BELOW_250 = new Rule(-200, "ЗП ниже 250k");
  public static final Rule COMPENSATION_TYPE_WHITE = new Rule(0, "Белая ЗП");
  public static final Rule COMPENSATION_TYPE_GRAY = new Rule(-200, "Серая ЗП");
  public static final Rule COMPENSATION_TYPE_NONE = new Rule(0); // тип не указан - не показываем
  public static final Rule COMPENSATION_EQUITY = new Rule(100, "Опционы/акции");

  // ============ BENEFITS ============
  public static final Rule BENEFITS_HEALTH_INSURANCE = new Rule(30, "ДМС");
  public static final Rule BENEFITS_EXTENDED_VACATION = new Rule(40, "Расширенный отпуск");
  public static final Rule BENEFITS_WELLNESS = new Rule(25, "Компенсация спорта/здоровья");
  public static final Rule BENEFITS_REMOTE_COMPENSATION = new Rule(35, "Компенсация удаленки");
  public static final Rule BENEFITS_EDUCATION = new Rule(50, "Компенсация обучения");
  public static final Rule BENEFITS_CONFERENCES = new Rule(40, "Бюджет на конференции");
  public static final Rule BENEFITS_INTERNAL_TRAINING = new Rule(20, "Внутреннее обучение");
  public static final Rule BENEFITS_PAID_SICK_LEAVE = new Rule(25, "Оплачиваемый больничный");

  // ============ EQUIPMENT ============
  // BYOD
  public static final Rule EQUIPMENT_BYOD_YES = new Rule(20, "Можно работать на своём ноутбуке");
  public static final Rule EQUIPMENT_BYOD_NO = new Rule(0); // не показываем
  public static final Rule EQUIPMENT_BYOD_NONE = new Rule(0); // не показываем

  // macOS
  public static final Rule EQUIPMENT_MACOS_PROVIDED = new Rule(30, "Выдают MacBook");
  public static final Rule EQUIPMENT_MACOS_ALLOWED = new Rule(20, "Можно работать на Mac");
  public static final Rule EQUIPMENT_MACOS_BOTH = new Rule(40, "Выдают MacBook или можно свой Mac");
  public static final Rule EQUIPMENT_MACOS_NONE = new Rule(0); // не показываем

  // Компенсация
  public static final Rule EQUIPMENT_COMPENSATION_FULL = new Rule(60, "Полная компенсация техники");
  public static final Rule EQUIPMENT_COMPENSATION_PARTIAL = new Rule(40, "Частичная компенсация техники");
  public static final Rule EQUIPMENT_COMPENSATION_NONE = new Rule(0); // не показываем

  // ============ INDUSTRY ============
  // Компании
  public static final Rule INDUSTRY_COMPANY_SAFE = new Rule(80, "Полезная компания");
  public static final Rule INDUSTRY_COMPANY_NEUTRAL = new Rule(0);
  public static final Rule INDUSTRY_COMPANY_TOXIC = new Rule(-150, "Токсичная компания");

  // Проекты
  public static final Rule INDUSTRY_PROJECT_SAFE = new Rule(60, "Полезный проект");
  public static final Rule INDUSTRY_PROJECT_TOXIC = new Rule(-100, "Токсичный проект");

  // Направления
  public static final Rule INDUSTRY_DIRECTION_HEALTHCARE = new Rule(20, "Медицина");
  public static final Rule INDUSTRY_DIRECTION_EDUCATION = new Rule(20, "Образование");
  public static final Rule INDUSTRY_DIRECTION_ENERGY = new Rule(30, "Энергетика/ВИЭ");
  public static final Rule INDUSTRY_DIRECTION_HARMFUL = new Rule(-50, "Вредная индустрия");

  // ============ WORK_CONDITIONS ============
  public static final Rule WORK_CONDITIONS_REMOTE = new Rule(20, "Полностью удаленно");
  public static final Rule WORK_CONDITIONS_REMOTE_GLOBAL = new Rule(100, "Удаленка из любой страны");
  public static final Rule WORK_CONDITIONS_REMOTE_RESTRICTED = new Rule(30, "Удаленка с ограничениями");
  public static final Rule WORK_CONDITIONS_HYBRID_FLEXIBLE = new Rule(20, "Гибрид с гибким графиком");
  public static final Rule WORK_CONDITIONS_HYBRID = new Rule(20, "Гибридный формат");
  public static final Rule WORK_CONDITIONS_HYBRID_FREQUENT = new Rule(0, "Частые визиты в офис");
  public static final Rule WORK_CONDITIONS_OFFICE = new Rule(-100, "Только офис");
  public static final Rule WORK_CONDITIONS_RELOCATION_REQUIRED = new Rule(-100, "Обязательный релокейт");
  public static final Rule WORK_CONDITIONS_RELOCATION_ASSISTED = new Rule(-100, "Релокейт с поддержкой");

  // ============ STOP_FACTORS ============
  public static final Rule STOP_FACTORS_TOXIC_CULTURE = new Rule(-300, "Токсичная культура");
  public static final Rule STOP_FACTORS_BANNED_DOMAIN = new Rule(-500, "Запрещенная сфера");
}