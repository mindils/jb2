package ru.mindils.jb2.app.entity;

import io.jmix.core.metamodel.datatype.EnumClass;

import org.springframework.lang.Nullable;


public enum VacancyLlmAnalysisType implements EnumClass<String> {

  JAVA_PRIMARY("JAVA_PRIMARY"),
    STOP_FACTORS("STOP_FACTORS"),
    BENEFITS("BENEFITS"),
    COMPENSATION("COMPENSATION"),
    EQUIPMENT("EQUIPMENT"),
    INDUSTRY("INDUSTRY"),
    TECHNICAL("TECHNICAL"),
    WORK_CONDITIONS("WORK_CONDITIONS");

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