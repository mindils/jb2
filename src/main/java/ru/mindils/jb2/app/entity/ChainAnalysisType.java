package ru.mindils.jb2.app.entity;

import io.jmix.core.metamodel.datatype.EnumClass;
import org.springframework.lang.Nullable;

/**
 * Типы цепочек анализа вакансий
 */
public enum ChainAnalysisType implements EnumClass<String> {

  FULL_ANALYSIS("FULL_ANALYSIS"),           // полный анализ
  PRIMARY_ONLY("PRIMARY_ONLY"),             // только первичный
  SOCIAL_TECHNICAL("SOCIAL_TECHNICAL"),     // социальный + технический
  TECHNICAL_ONLY("TECHNICAL_ONLY"),         // только технический
  CUSTOM("CUSTOM");                         // настраиваемая цепочка

  private final String id;

  ChainAnalysisType(String id) {
    this.id = id;
  }

  @Override
  public String getId() {
    return id;
  }

  @Nullable
  public static ChainAnalysisType fromId(String id) {
    for (ChainAnalysisType type : ChainAnalysisType.values()) {
      if (type.getId().equals(id)) {
        return type;
      }
    }
    return null;
  }
}