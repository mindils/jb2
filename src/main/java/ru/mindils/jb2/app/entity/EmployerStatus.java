package ru.mindils.jb2.app.entity;

import io.jmix.core.metamodel.datatype.EnumClass;

import org.springframework.lang.Nullable;


public enum EmployerStatus implements EnumClass<String> {

  NEW("NEW"),
    DECLINED("DECLINED"),
    APPROVED("APPROVED");

    private final String id;

    EmployerStatus(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Nullable
    public static EmployerStatus fromId(String id) {
        for (EmployerStatus at : EmployerStatus.values()) {
            if (at.getId().equals(id)) {
                return at;
            }
        }
        return null;
    }
}