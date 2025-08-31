package ru.mindils.jb2.app.entity;

import io.jmix.core.metamodel.datatype.EnumClass;

import org.springframework.lang.Nullable;


public enum VacancyStatus implements EnumClass<String> {

    NEW("NEW"),
    IN_PROGRESS("IN_PROGRESS"),
    ARCHIVED("ARCHIVED"),
    DECLINED("DECLINED"),
    APPROVED("APPROVED");

    private final String id;

    VacancyStatus(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Nullable
    public static VacancyStatus fromId(String id) {
        for (VacancyStatus at : VacancyStatus.values()) {
            if (at.getId().equals(id)) {
                return at;
            }
        }
        return null;
    }
}