package ru.mindils.jb2.app.entity;

import io.jmix.core.metamodel.datatype.EnumClass;
import org.springframework.lang.Nullable;

public enum VacancyRating implements EnumClass<String> {
  EXCELLENT("EXCELLENT"),
  GOOD("GOOD"),
  MODERATE("MODERATE"),
  POOR("POOR"),
  VERY_POOR("VERY_POOR");

  private final String id;

  VacancyRating(String id) {
    this.id = id;
  }

  @Override
  public String getId() {
    return id;
  }

  @Nullable
  public static VacancyRating fromId(String id) {
    for (VacancyRating rating : VacancyRating.values()) {
      if (rating.getId().equals(id)) {
        return rating;
      }
    }
    return null;
  }
}