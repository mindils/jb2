package ru.mindils.jb2.app.entity;

import io.jmix.core.metamodel.annotation.JmixEntity;
import io.jmix.data.DdlGeneration;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.OffsetDateTime;

@JmixEntity
@Table(name = "JB2_VACANCY_INFO")
@DdlGeneration(unmappedConstraints = {"FK_JB2_VACANCY_INFO_ON_ID"})
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

  @CreatedDate
  @Column(name = "CREATED_DATE")
  private OffsetDateTime createdDate;

  @LastModifiedDate
  @Column(name = "LAST_MODIFIED_DATE")
  private OffsetDateTime lastModifiedDate;

  public VacancyStatus getStatus() {
    if (status == null) {
      return VacancyStatus.NEW;  // ✅ Возвращаем NOT_SET вместо null
    }
    return VacancyStatus.fromId(status);
  }

  public void setStatus(VacancyStatus status) {
    if (status == VacancyStatus.NEW) {
      this.status = null;
    } else {
      this.status = status == null ? null : status.getId();
    }
  }
}