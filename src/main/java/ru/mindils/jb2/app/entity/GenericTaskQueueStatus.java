package ru.mindils.jb2.app.entity;

import io.jmix.core.metamodel.datatype.EnumClass;
import org.springframework.lang.Nullable;

public enum GenericTaskQueueStatus implements EnumClass<String> {

  NEW("NEW"),
  PROCESSING("PROCESSING"),
  COMPLETED("COMPLETED"),
  FAILED("FAILED");

  private final String id;

  GenericTaskQueueStatus(String id) {
    this.id = id;
  }

  @Override
  public String getId() {
    return id;
  }

  @Nullable
  public static GenericTaskQueueStatus fromId(String id) {
    for (GenericTaskQueueStatus status : GenericTaskQueueStatus.values()) {
      if (status.getId().equals(id)) {
        return status;
      }
    }
    return null;
  }
}