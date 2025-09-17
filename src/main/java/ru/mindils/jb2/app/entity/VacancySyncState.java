package ru.mindils.jb2.app.entity;

import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.OffsetDateTime;

@JmixEntity
@Table(name = "jb2_vacancy_sync_state")
@Entity(name = "jb2_VacancySyncState")
@Getter
@Setter
public class VacancySyncState {
  @Column(name = "id", nullable = false)
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "type_state")
  private String stateType;

  @CreatedDate
  @Column(name = "created_date")
  private OffsetDateTime createdDate;

  @LastModifiedDate
  @Column(name = "last_modified_date")
  private OffsetDateTime lastModifiedDate;

  public void setStateType(VacancySyncStateType stateType) {
    this.stateType = stateType == null ? null : stateType.getId();
  }

  public VacancySyncStateType getStateType() {
    return stateType == null ? null : VacancySyncStateType.fromId(stateType);
  }
}