package ru.mindils.jb2.app.service.analysis;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Правила оценки вакансий
 * Каждое правило содержит оценку и описание
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

  // ============ PRIMARY (основные требования) ============
  // Java - это базовое требование, не показываем в описании
  public static final Rule JAVA = new Rule(0);

  // Jmix - показываем, это важная особенность
  public static final Rule JMIX = new Rule(150, "Jmix фреймворк");

  public static final Rule AI_ALLOWED = new Rule(30, "AI инструменты разрешены");
  public static final Rule AI_PROJECT_OPTIONAL = new Rule(40, "AI проекты опционально");
  public static final Rule AI_PROJECT_REQUIRED = new Rule(-50, "AI проекты обязательны");

  // ============ SOCIAL (формат работы) ============
  public static final Rule REMOTE = new Rule(20, "Полностью удаленно");
  public static final Rule FLEXIBLE = new Rule(20, "Гибкий график");
  public static final Rule HYBRID_FLEXIBLE = new Rule(20, "Гибрид с гибким графиком");
  public static final Rule HYBRID = new Rule(20, "Гибридный формат");
  public static final Rule HYBRID_2_3 = new Rule(20, "Гибрид 2/3");
  public static final Rule HYBRID_3_2 = new Rule(20, "Гибрид 3/2");
  public static final Rule HYBRID_4_1 = new Rule(0, "Гибрид 4/1");
  public static final Rule OFFICE = new Rule(-100, "Только офис");
  public static final Rule SOCIALLY_SIGNIFICANT = new Rule(50, "Социально значимый проект");

  // ============ TECHNICAL (роль и уровень) ============
  // Backend - базовое, без описания
  public static final Rule BACKEND = new Rule(50);
  public static final Rule FRONTEND_BACKEND = new Rule(40, "Fullstack (Backend+Frontend)");
  public static final Rule DEVOPS_WITH_DEV = new Rule(30, "DevOps с разработкой");
  public static final Rule OTHER_ROLE = new Rule(-100, "Другая роль");

  // Уровень - тоже базовое, можно не показывать
  public static final Rule ARCHITECT = new Rule(20);
  public static final Rule PRINCIPAL = new Rule(20);
  public static final Rule SENIOR = new Rule(20);
  public static final Rule LEAD = new Rule(20);
  public static final Rule MIDDLE = new Rule(20);
  public static final Rule JUNIOR = new Rule(-10);

  // ============ TECHNICAL (стек) ============
  // Базовый стек - не показываем
  public static final Rule SPRING_STACK = new Rule(0);
  public static final Rule MICROSERVICES = new Rule(0);
  public static final Rule DATABASE = new Rule(0);

  // Интересные добавки - показываем
  public static final Rule PYTHON = new Rule(10, "Python в стеке");
  public static final Rule DEVOPS_STACK = new Rule(30, "DevOps технологии");
  public static final Rule FRONTEND_STACK = new Rule(30, "Frontend разработка");

  // ============ COMPENSATION (зарплата) ============
  public static final Rule SALARY_NOT_SPECIFIED = new Rule(0); // Зарплата не указана
  public static final Rule SALARY_HIGH_400 = new Rule(100, "Высокая ЗП (400k+)");
  public static final Rule SALARY_350_400 = new Rule(50, "ЗП 350-400k");
  public static final Rule SALARY_350_400_BONUS = new Rule(60, "ЗП 350-400k + бонусы");
  public static final Rule SALARY_300_350 = new Rule(0, "ЗП 300-350k");
  public static final Rule SALARY_300_350_BONUS = new Rule(20, "ЗП 300-350k + бонусы");
  public static final Rule SALARY_250_300 = new Rule(-100, "ЗП 250-300k");
  public static final Rule SALARY_BELOW_250 = new Rule(-200, "ЗП ниже 250k");
  public static final Rule WHITE_SALARY = new Rule(0, "Белая ЗП");
  public static final Rule GRAY_SALARY = new Rule(-200, "Серая ЗП");
  public static final Rule EQUITY = new Rule(100, "Опционы/акции");

  // ============ BENEFITS (льготы) ============
  public static final Rule HEALTH_INSURANCE = new Rule(30, "ДМС");
  public static final Rule EXTENDED_VACATION = new Rule(40, "Расширенный отпуск");
  public static final Rule WELLNESS = new Rule(25, "Компенсация спорта");
  public static final Rule COWORKING = new Rule(35, "Компенсация коворкинга");
  public static final Rule EDUCATION = new Rule(50, "Компенсация обучения");
  public static final Rule CONFERENCES = new Rule(40, "Бюджет на конференции");
  public static final Rule INTERNAL_TRAINING = new Rule(20, "Внутреннее обучение");
  public static final Rule PAID_SICK_LEAVE = new Rule(25, "Оплачиваемый больничный");

  // ============ EQUIPMENT (оборудование) ============
  public static final Rule MACBOOK_PRO = new Rule(10, "MacBook Pro");
  public static final Rule WINDOWS_LAPTOP = new Rule(10, "Windows ноутбук");
  public static final Rule BYOD_NO_COMP = new Rule(10, "Свой ноут без компенсации");
  public static final Rule BYOD_PARTIAL = new Rule(50, "Свой ноут, частичная компенсация");
  public static final Rule BYOD_FULL = new Rule(50, "Свой ноут, полная компенсация");
  public static final Rule MONITORS = new Rule(0); // Мониторы
  public static final Rule PERIPHERALS = new Rule(0); // Периферия

  // ============ INDUSTRY (отрасль) ============
  public static final Rule POSITIVE_COMPANY = new Rule(80, "Позитивная компания");
  public static final Rule NEUTRAL_COMPANY = new Rule(0, "Нейтральная компания");
  public static final Rule PROBLEMATIC_COMPANY = new Rule(-50, "Проблемная компания");
  public static final Rule TOXIC_COMPANY = new Rule(-150, "Токсичная компания");

  // ============ WORK CONDITIONS (условия) ============
  public static final Rule REMOTE_GLOBAL = new Rule(100, "Удаленка из любой страны");
  public static final Rule REMOTE_RESTRICTED = new Rule(30, "Удаленка с ограничениями");
  public static final Rule RELOCATION_REQUIRED = new Rule(-100, "Обязательный релокейт");
  public static final Rule RELOCATION_ASSISTED = new Rule(-100, "Релокейт с поддержкой");
  public static final Rule HYBRID_FREQUENT = new Rule(0, "Частые визиты в офис");

  // ============ STOP FACTORS (стоп-факторы) ============
  public static final Rule TOXIC_CULTURE = new Rule(-300, "Токсичная культура");
  public static final Rule BANNED_DOMAIN = new Rule(-500, "Запрещенная сфера");
}