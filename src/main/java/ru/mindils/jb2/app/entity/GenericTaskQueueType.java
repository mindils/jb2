package ru.mindils.jb2.app.entity;

import io.jmix.core.metamodel.datatype.EnumClass;
import org.springframework.lang.Nullable;

/**
 * Определяет тип анализа вакансии, который необходимо выполнить.
 */
public enum GenericTaskQueueType implements EnumClass<String> {

  LLM_FIRST("LLM_FIRST");

  private final String id;

  GenericTaskQueueType(String id) {
    this.id = id;
  }

  @Override
  public String getId() {
    return id;
  }

  @Nullable
  public static GenericTaskQueueType fromId(String id) {
    for (GenericTaskQueueType at : GenericTaskQueueType.values()) {
      if (at.getId().equals(id)) {
        return at;
      }
    }
    return null;
  }
}