package ru.mindils.jb2.app.entity;

import io.jmix.core.metamodel.datatype.EnumClass;
import org.springframework.lang.Nullable;


public enum VacancyLlmAnalysisStatus implements EnumClass<String> {

  DONE("DONE"),
  SKIPPED("SKIPPED"),
  ERROR("ERROR");

  private final String id;

  VacancyLlmAnalysisStatus(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }

  @Nullable
  public static VacancyLlmAnalysisStatus fromId(String id) {
    for (VacancyLlmAnalysisStatus at : VacancyLlmAnalysisStatus.values()) {
      if (at.getId().equals(id)) {
        return at;
      }
    }
    return null;
  }
}