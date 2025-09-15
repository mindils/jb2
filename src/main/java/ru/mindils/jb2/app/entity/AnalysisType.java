package ru.mindils.jb2.app.entity;

import io.jmix.core.metamodel.datatype.EnumClass;
import org.springframework.lang.Nullable;

/**
 * Определяет тип анализа вакансии, который необходимо выполнить.
 */
public enum AnalysisType implements EnumClass<String> {

  /**
   * Первичный анализ: Java, Jmix, AI.
   */
  PRIMARY("PRIMARY"),

  /**
   * Социальный анализ: режим работы, домены, соц. значимость.
   */
  SOCIAL("SOCIAL");

  private final String id;

  AnalysisType(String id) {
    this.id = id;
  }

  @Override
  public String getId() {
    return id;
  }

  @Nullable
  public static AnalysisType fromId(String id) {
    for (AnalysisType at : AnalysisType.values()) {
      if (at.getId().equals(id)) {
        return at;
      }
    }
    return null;
  }
}