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

  /**
   * Основной язык - Java (базовый вариант, нейтрально)
   */
  public static final Rule JAVA_PRIMARY_JAVA = new Rule(0);

  /**
   * Используется Jmix фреймворк (большой плюс)
   */
  public static final Rule JAVA_PRIMARY_JMIX = new Rule(150, "Jmix");

  // ============ TECHNICAL ============
  // Роли

  /**
   * Backend разработчик (базовая роль)
   */
  public static final Rule TECHNICAL_ROLE_BACKEND = new Rule(0);

  /**
   * Fullstack разработчик (фронт+бэк)
   */
  public static final Rule TECHNICAL_ROLE_FULLSTACK = new Rule(0);

  /**
   * DevOps с разработкой (инфра+код)
   */
  public static final Rule TECHNICAL_ROLE_DEVOPS = new Rule(0);

  /**
   * Другая роль, не подходящая под критерии
   */
  public static final Rule TECHNICAL_ROLE_OTHER = new Rule(-100, "Др. роль");

  /**
   * Роль не указана в вакансии
   */
  public static final Rule TECHNICAL_ROLE_NONE = new Rule(0);

  // Уровни

  /**
   * Уровень Architect
   */
  public static final Rule TECHNICAL_LEVEL_ARCHITECT = new Rule(0);

  /**
   * Уровень Principal
   */
  public static final Rule TECHNICAL_LEVEL_PRINCIPAL = new Rule(0);

  /**
   * Уровень Senior
   */
  public static final Rule TECHNICAL_LEVEL_SENIOR = new Rule(0);

  /**
   * Уровень Lead
   */
  public static final Rule TECHNICAL_LEVEL_LEAD = new Rule(0);

  /**
   * Уровень Middle
   */
  public static final Rule TECHNICAL_LEVEL_MIDDLE = new Rule(0);

  /**
   * Уровень Junior (не желательно)
   */
  public static final Rule TECHNICAL_LEVEL_JUNIOR = new Rule(-20);

  /**
   * Уровень не указан
   */
  public static final Rule TECHNICAL_LEVEL_NONE = new Rule(0);

  // Стек

  /**
   * Spring в стеке
   */
  public static final Rule TECHNICAL_STACK_SPRING = new Rule(0);

  /**
   * Микросервисная архитектура
   */
  public static final Rule TECHNICAL_STACK_MICROSERVICES = new Rule(0);

  /**
   * Работа с БД
   */
  public static final Rule TECHNICAL_STACK_DATABASE = new Rule(0);

  /**
   * Python дополнительно в стеке
   */
  public static final Rule TECHNICAL_STACK_PYTHON = new Rule(0);

  /**
   * DevOps технологии в стеке
   */
  public static final Rule TECHNICAL_STACK_DEVOPS = new Rule(0);

  /**
   * Frontend разработка в стеке (плюс)
   */
  public static final Rule TECHNICAL_STACK_FRONTEND = new Rule(30);

  /**
   * Стек не указан
   */
  public static final Rule TECHNICAL_STACK_NONE = new Rule(0);

  // AI

  /**
   * AI инструменты разрешены (плюс)
   */
  public static final Rule TECHNICAL_AI_ALLOWED = new Rule(30, "AI разрешен");

  /**
   * AI проекты опционально (плюс)
   */
  public static final Rule TECHNICAL_AI_PROJECT_OPTIONAL = new Rule(40, "AI опционально");

  /**
   * AI проекты обязательны (минус)
   */
  public static final Rule TECHNICAL_AI_PROJECT_REQUIRED = new Rule(-50, "AI обязателен");

  /**
   * AI не упоминается
   */
  public static final Rule TECHNICAL_AI_NONE = new Rule(0);

  // ============ COMPENSATION ============

  /**
   * Зарплата не указана
   */
  public static final Rule COMPENSATION_NONE = new Rule(0);

  /**
   * Высокая ЗП от 400k
   */
  public static final Rule COMPENSATION_HIGH_400 = new Rule(100, "ЗП 400k+");

  /**
   * ЗП 350-400k
   */
  public static final Rule COMPENSATION_350_400 = new Rule(50, "ЗП 350-400k");

  /**
   * ЗП 350-400k с бонусами
   */
  public static final Rule COMPENSATION_350_400_BONUS = new Rule(60, "ЗП 350-400k+бонус");

  /**
   * ЗП 300-350k
   */
  public static final Rule COMPENSATION_300_350 = new Rule(0, "ЗП 300-350k");

  /**
   * ЗП 300-350k с бонусами
   */
  public static final Rule COMPENSATION_300_350_BONUS = new Rule(20, "ЗП 300-350k+бонус");

  /**
   * ЗП 250-300k (низковато)
   */
  public static final Rule COMPENSATION_250_300 = new Rule(-100, "ЗП 250-300k");

  /**
   * ЗП ниже 250k (слишком низко)
   */
  public static final Rule COMPENSATION_BELOW_250 = new Rule(-200, "ЗП <250k");

  /**
   * Белая зарплата (официальная)
   */
  public static final Rule COMPENSATION_TYPE_WHITE = new Rule(0, "Белая ЗП");

  /**
   * Серая зарплата (большой минус)
   */
  public static final Rule COMPENSATION_TYPE_GRAY = new Rule(-200, "Серая ЗП");

  /**
   * Тип выплаты не указан
   */
  public static final Rule COMPENSATION_TYPE_NONE = new Rule(0);

  /**
   * Опционы или акции компании (большой плюс)
   */
  public static final Rule COMPENSATION_EQUITY = new Rule(100, "Опционы");

  // ============ BENEFITS ============

  /**
   * Добровольное медицинское страхование
   */
  public static final Rule BENEFITS_HEALTH_INSURANCE = new Rule(30, "ДМС");

  /**
   * Расширенный отпуск (больше стандартного)
   */
  public static final Rule BENEFITS_EXTENDED_VACATION = new Rule(40, "Доп. отпуск");

  /**
   * Компенсация спорта/здоровья
   */
  public static final Rule BENEFITS_WELLNESS = new Rule(25, "Комп. спорта");

  /**
   * Компенсация удаленной работы
   */
  public static final Rule BENEFITS_REMOTE_COMPENSATION = new Rule(0);

  /**
   * Компенсация обучения и курсов
   */
  public static final Rule BENEFITS_EDUCATION = new Rule(30, "Комп. обуч.");

  /**
   * Бюджет на посещение конференций
   */
  public static final Rule BENEFITS_CONFERENCES = new Rule(10, "Конференции");

  /**
   * Внутренние тренинги и обучение
   */
  public static final Rule BENEFITS_INTERNAL_TRAINING = new Rule(20, "Внутр. обуч.");

  /**
   * Оплачиваемый больничный
   */
  public static final Rule BENEFITS_PAID_SICK_LEAVE = new Rule(25, "Опл. больничн.");

  // ============ EQUIPMENT ============
  // BYOD (Bring Your Own Device)

  /**
   * Можно работать на своем ноутбуке
   */
  public static final Rule EQUIPMENT_BYOD_YES = new Rule(40, "Свой ноут");

  /**
   * BYOD запрещен
   */
  public static final Rule EQUIPMENT_BYOD_NO = new Rule(0);

  /**
   * BYOD не упоминается
   */
  public static final Rule EQUIPMENT_BYOD_NONE = new Rule(0);

  // macOS

  /**
   * Компания предоставляет MacBook
   */
  public static final Rule EQUIPMENT_MACOS_PROVIDED = new Rule(40, "MacBook выдают");

  /**
   * Можно использовать Mac
   */
  public static final Rule EQUIPMENT_MACOS_ALLOWED = new Rule(40, "Mac разрешен");

  /**
   * Выдают MacBook или можно свой Mac
   */
  public static final Rule EQUIPMENT_MACOS_BOTH = new Rule(40, "Mac");

  /**
   * macOS не упоминается
   */
  public static final Rule EQUIPMENT_MACOS_NONE = new Rule(0);

  // Компенсация техники

  /**
   * Полная компенсация покупки техники
   */
  public static final Rule EQUIPMENT_COMPENSATION_FULL = new Rule(40, "100% комп. тех.");

  /**
   * Частичная компенсация техники
   */
  public static final Rule EQUIPMENT_COMPENSATION_PARTIAL = new Rule(40, "Част. комп. тех.");

  /**
   * Компенсация не предоставляется
   */
  public static final Rule EQUIPMENT_COMPENSATION_NONE = new Rule(0);

  // ============ INDUSTRY ============
  // Компании

  /**
   * Социально-полезная компания (большой плюс)
   */
  public static final Rule INDUSTRY_COMPANY_SAFE = new Rule(80, "Полезн. комп.");

  /**
   * Нейтральная компания (обычный бизнес)
   */
  public static final Rule INDUSTRY_COMPANY_NEUTRAL = new Rule(0);

  /**
   * Проблемная компания (этические вопросы)
   */
  public static final Rule INDUSTRY_COMPANY_PROBLEMATIC = new Rule(-50, "Пробл. комп.");

  /**
   * Токсичная компания (сильный минус)
   */
  public static final Rule INDUSTRY_COMPANY_TOXIC = new Rule(-150, "Токс. комп.");

  // Проекты

  /**
   * Социально-полезный проект
   */
  public static final Rule INDUSTRY_PROJECT_SAFE = new Rule(60, "Полезн. проект");

  /**
   * Проблемный проект
   */
  public static final Rule INDUSTRY_PROJECT_PROBLEMATIC = new Rule(-60, "Пробл. проект");

  /**
   * Токсичный проект
   */
  public static final Rule INDUSTRY_PROJECT_TOXIC = new Rule(-100, "Токс. проект");

  // Направления проектов

  /**
   * Проект в области медицины/здравоохранения
   */
  public static final Rule INDUSTRY_DIRECTION_HEALTHCARE = new Rule(40, "Проект: медицина");

  /**
   * Проект в области образования
   */
  public static final Rule INDUSTRY_DIRECTION_EDUCATION = new Rule(40, "Проект: образ.");

  /**
   * Проект в области энергетики/ВИЭ
   */
  public static final Rule INDUSTRY_DIRECTION_ENERGY = new Rule(40, "Проект: энергетика");

  /**
   * Проект в социально-вредной индустрии
   */
  public static final Rule INDUSTRY_DIRECTION_HARMFUL = new Rule(-50, "Вредн. индустрия");

  // ============ WORK_CONDITIONS ============

  /**
   * Полностью удаленная работа
   */
  public static final Rule WORK_CONDITIONS_REMOTE = new Rule(0, "Удаленка");

  /**
   * Удаленка из любой точки мира (большой плюс)
   */
  public static final Rule WORK_CONDITIONS_REMOTE_GLOBAL = new Rule(100, "Удаленка из любой страны");

  /**
   * Удаленка с ограничениями по локации
   */
  public static final Rule WORK_CONDITIONS_REMOTE_RESTRICTED = new Rule(0, "Удаленка огранич.");

  /**
   * Гибрид с гибким графиком посещения
   */
  public static final Rule WORK_CONDITIONS_HYBRID_FLEXIBLE = new Rule(0, "Гибрид гибк.");

  /**
   * Гибридный формат (офис + удаленка)
   */
  public static final Rule WORK_CONDITIONS_HYBRID = new Rule(0, "Гибрид");

  /**
   * Гибрид с частыми визитами в офис
   */
  public static final Rule WORK_CONDITIONS_HYBRID_FREQUENT = new Rule(0, "Гибрид част.");

  /**
   * Только офисная работа (минус)
   */
  public static final Rule WORK_CONDITIONS_OFFICE = new Rule(-100, "Только офис");

  /**
   * Обязательный релокейт (минус)
   */
  public static final Rule WORK_CONDITIONS_RELOCATION_REQUIRED = new Rule(-100, "Релок. обяз.");

  /**
   * Релокейт с поддержкой компании (все равно минус)
   */
  public static final Rule WORK_CONDITIONS_RELOCATION_ASSISTED = new Rule(-100, "Релок. с поддерж.");

  // ============ STOP_FACTORS ============

  /**
   * Токсичная культура компании (критический минус)
   */
  public static final Rule STOP_FACTORS_TOXIC_CULTURE = new Rule(-300, "Токс. культура");

  /**
   * Запрещенная сфера деятельности (критический минус)
   */
  public static final Rule STOP_FACTORS_BANNED_DOMAIN = new Rule(-500, "Запрещ. сфера");
}