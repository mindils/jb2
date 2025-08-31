package ru.mindils.jb2.app.entity;

import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@JmixEntity
@Table(name = "JB2_VACANCY_FILTER_PARAMS", indexes = {
        @Index(name = "IDX_JB2_VACANCY_FILTER_PARAMS_VACANCY_FILTER", columnList = "VACANCY_FILTER_ID")
})
@Entity(name = "jb2_VacancyFilterParams")
@Getter
@Setter
public class VacancyFilterParams {
    @Id
    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JoinColumn(name = "VACANCY_FILTER_ID")
    @ManyToOne(fetch = FetchType.LAZY)
    private VacancyFilter vacancyFilter;

    @Column(name = "PARAM_NAME")
    private String paramName;

    @Column(name = "PARAM_VALUE")
    private String paramValue;
}