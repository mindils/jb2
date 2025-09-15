package ru.mindils.jb2.app.entity;

import io.jmix.core.metamodel.datatype.EnumClass;

import org.springframework.lang.Nullable;


public enum VacancyAnalysisQueueType implements EnumClass<String> {

  FIRST("FIRST"),
    SOCIAL("SOCIAL");

  private final String id;

  VacancyAnalysisQueueType(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }

  @Nullable
  public static VacancyAnalysisQueueType fromId(String id) {
    for (VacancyAnalysisQueueType at : VacancyAnalysisQueueType.values()) {
      if (at.getId().equals(id)) {
        return at;
      }
    }
    return null;
  }
}