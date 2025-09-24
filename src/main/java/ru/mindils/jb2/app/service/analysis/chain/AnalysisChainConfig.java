package ru.mindils.jb2.app.service.analysis.chain;

import ru.mindils.jb2.app.entity.ChainAnalysisType;

import java.util.List;

/**
 * Конфигурация цепочки анализа
 */
public record AnalysisChainConfig(
    ChainAnalysisType chainId,
    String description,
    List<String> stepIds,      // последовательность ID шагов
    boolean calculateScore,    // выполнять ли итоговый расчет скора
    boolean forceReanalyze     // перезаписывать ли уже существующие результаты (true) или использовать кэшированные (false)
) {

  // Предопределенные конфигурации
  public static final AnalysisChainConfig FULL_ANALYSIS = new AnalysisChainConfig(
      ChainAnalysisType.FULL_ANALYSIS,
      "Полный анализ: первичный + социальный + технический + скор",
      List.of(
          "primary", // Первичный анализ: определение Java стек
          "stopFactors", // Проверка критических стоп-факторов

          "benefits", // Анализ льгот и дополнительных преимуществ
          "compensation", // Анализ компенсации и структуры зарплаты
          "equipment", // Анализ предоставляемого оборудования и технических средств
          "industry", // Анализ отрасли и социальной значимости
          "social", // Социальный анализ: формат работы, домены, социальная значимость
          "technical", // Технический анализ: роль, уровень, стек технологий
          "workConditions" // Анализ условий работы и требований к релокации
      ),
      true,
      false  // по умолчанию используем кэшированные результаты
  );

  public static final AnalysisChainConfig PRIMARY_ONLY = new AnalysisChainConfig(
      ChainAnalysisType.PRIMARY_ONLY,
      "Только первичный анализ + скор",
      List.of("primary"),
      true,
      false
  );

  public static final AnalysisChainConfig SOCIAL_TECHNICAL = new AnalysisChainConfig(
      ChainAnalysisType.SOCIAL_TECHNICAL,
      "Социальный + технический анализ (первичный уже выполнен)",
      List.of("social", "technical"),
      true,
      false
  );

  // Конфигурации с принудительным перезапуском
  public static final AnalysisChainConfig FULL_ANALYSIS_FORCE = new AnalysisChainConfig(
      ChainAnalysisType.FULL_ANALYSIS,
      "Полный анализ с перезапуском LLM",
      List.of("primary", "technical"),
      true,
      true   // принудительно перезапускаем LLM
  );

  public static final AnalysisChainConfig PRIMARY_ONLY_FORCE = new AnalysisChainConfig(
      ChainAnalysisType.PRIMARY_ONLY,
      "Первичный анализ с перезапуском LLM",
      List.of("primary"),
      true,
      true
  );

  /**
   * Создать конфигурацию с принудительным перезапуском
   */
  public AnalysisChainConfig withForceReanalyze(boolean forceReanalyze) {
    return new AnalysisChainConfig(
        this.chainId,
        this.description + (forceReanalyze ? " (с перезапуском)" : " (с кэшированием)"),
        this.stepIds,
        this.calculateScore,
        forceReanalyze
    );
  }
}