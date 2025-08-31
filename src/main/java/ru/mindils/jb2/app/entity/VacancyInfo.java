package ru.mindils.jb2.app.entity;

import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@JmixEntity
@Table(name = "JB2_VACANCY_INFO")
@Entity(name = "jb2_VacancyInfo")
@Getter
@Setter
public class VacancyInfo {

    @Id
    @Column(name = "ID", nullable = false)
    private String id;

    @Column(name = "STATUS")
    private String status;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID",
            foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT))
    @PrimaryKeyJoinColumn
    private Vacancy vacancy;

    public VacancyStatus getStatus() {
        return status == null ? null : VacancyStatus.fromId(status);
    }

    public void setStatus(VacancyStatus status) {
        this.status = status == null ? null : status.getId();
    }
}