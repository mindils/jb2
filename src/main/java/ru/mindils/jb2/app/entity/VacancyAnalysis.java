package ru.mindils.jb2.app.entity;

import io.jmix.core.metamodel.annotation.JmixEntity;
import io.jmix.data.DdlGeneration;
import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.OffsetDateTime;


@JmixEntity
@Table(name = "JB2_VACANCY_ANALYSIS")
@Entity(name = "jb2_VacancyAnalysis")
@DdlGeneration(unmappedConstraints = {"FK_JB2_VACANCY_ANALYSIS_ON_ID"})
@Getter
@Setter
public class VacancyAnalysis {
  @Id
  @Column(name = "id", nullable = false)
  private String id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id", foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT))
  @PrimaryKeyJoinColumn
  private Vacancy vacancy;

  @Column(name = "java")
  private String java;

  @Column(name = "jmix")
  private String jmix;

  @Column(name = "ai")
  private String ai;

  @Column(name = "DOMAINS")
  private String domains;

  @Column(name = "socially_significant")
  private String sociallySignificant;

  @Column(name = "work_mode")
  private String workMode;

  @CreatedDate
  @Column(name = "created_date")
  private OffsetDateTime createdDate;

  @LastModifiedDate
  @Column(name = "last_modified_date")
  private OffsetDateTime lastModifiedDate;

}