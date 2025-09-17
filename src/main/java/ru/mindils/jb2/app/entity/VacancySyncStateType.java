package ru.mindils.jb2.app.entity;

import io.jmix.core.metamodel.datatype.EnumClass;

import org.springframework.lang.Nullable;


public enum VacancySyncStateType implements EnumClass<String> {

  ALL_SYNC("ALL_SYNC");

  private final String id;

  VacancySyncStateType(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }

  @Nullable
  public static VacancySyncStateType fromId(String id) {
    for (VacancySyncStateType at : VacancySyncStateType.values()) {
      if (at.getId().equals(id)) {
        return at;
      }
    }
    return null;
  }
}