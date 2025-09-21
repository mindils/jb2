package ru.mindils.jb2.app.entity;

import io.jmix.core.metamodel.annotation.JmixEntity;
import io.jmix.data.DdlGeneration;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.OffsetDateTime;

@DdlGeneration(unmappedConstraints = {"FK_JB2_VACANCY_CHAIN_ANALYSIS_QUEUE_ON_VACANCY"})
@JmixEntity
@Table(name = "JB2_VACANCY_CHAIN_ANALYSIS_QUEUE", indexes = {
    @Index(name = "IDX_JB2_VACANCY_CHAIN_ANALYSIS_QUEUE_VACANCY", columnList = "vacancy_id"),
    @Index(name = "IDX_JB2_VACANCY_CHAIN_ANALYSIS_QUEUE_CHAIN_TYPE", columnList = "chain_type"),
    @Index(name = "IDX_JB2_VACANCY_CHAIN_ANALYSIS_QUEUE_PROCESSING", columnList = "processing")
})
@Entity(name = "jb2_VacancyChainAnalysisQueue")
@Getter
@Setter
public class VacancyChainAnalysisQueue {

  @Id
  @Column(name = "id", nullable = false)
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @JoinColumn(name = "vacancy_id")
  @ManyToOne(fetch = FetchType.LAZY)
  private Vacancy vacancy;

  @Column(name = "chain_type")
  private String chainType;

  @Column(name = "processing")
  private Boolean processing;

  @Column(name = "success")
  private Boolean success;

  @Column(name = "error_message")
  @Lob
  private String errorMessage;

  @Column(name = "priority")
  private Integer priority = 0; // для приоритизации

  @CreatedDate
  @Column(name = "created_date")
  private OffsetDateTime createdDate;

  @LastModifiedDate
  @Column(name = "last_modified_date")
  private OffsetDateTime lastModifiedDate;

  public void setChainType(ChainAnalysisType chainType) {
    this.chainType = chainType == null ? null : chainType.getId();
  }

  public ChainAnalysisType getChainType() {
    return chainType == null ? null : ChainAnalysisType.fromId(chainType);
  }
}