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
    boolean calculateScore     // выполнять ли итоговый расчет скора
) {

  // Предопределенные конфигурации
  public static final AnalysisChainConfig FULL_ANALYSIS = new AnalysisChainConfig(
      ChainAnalysisType.FULL_ANALYSIS,
      "Полный анализ: первичный + социальный + технический + скор",
//      List.of("primary", "social", "technical"),
      List.of("primary", "technical"),
      true
  );

  public static final AnalysisChainConfig PRIMARY_ONLY = new AnalysisChainConfig(
      ChainAnalysisType.PRIMARY_ONLY,
      "Только первичный анализ + скор",
      List.of("primary"),
      true
  );

  public static final AnalysisChainConfig SOCIAL_TECHNICAL = new AnalysisChainConfig(
      ChainAnalysisType.SOCIAL_TECHNICAL,
      "Социальный + технический анализ (первичный уже выполнен)",
      List.of("social", "technical"),
      true
  );
}
