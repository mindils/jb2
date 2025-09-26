package ru.mindils.jb2.app.entity;

import io.jmix.core.metamodel.datatype.EnumClass;

import org.springframework.lang.Nullable;


public enum VacancyLlmAnalysisType implements EnumClass<String> {

  JAVA_PRIMARY("JAVA_PRIMARY");

  private final String id;

  VacancyLlmAnalysisType(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }

  @Nullable
  public static VacancyLlmAnalysisType fromId(String id) {
    for (VacancyLlmAnalysisType at : VacancyLlmAnalysisType.values()) {
      if (at.getId().equals(id)) {
        return at;
      }
    }
    return null;
  }
}