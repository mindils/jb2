package ru.mindils.jb2.app.entity;

import io.jmix.core.metamodel.annotation.JmixEntity;
import io.jmix.data.DdlGeneration;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.OffsetDateTime;

@DdlGeneration(unmappedConstraints = {"FK_JB2_VACANCY_ANALYSIS_QUEUE_ON_VACANCY"})
@JmixEntity
@Table(name = "JB2_VACANCY_ANALYSIS_QUEUE", indexes = {
    @Index(name = "IDX_JB2_VACANCY_ANALYSIS_QUEUE_VACANCY", columnList = "vacancy_id")
})
@Entity(name = "jb2_VacancyAnalysisQueue")
@Getter
@Setter
public class VacancyAnalysisQueue {

  @Id
  @Column(name = "id", nullable = false)
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @JoinColumn(name = "vacancy_id")
  @ManyToOne(fetch = FetchType.LAZY)
  private Vacancy vacancy;

  @Column(name = "type_queue")
  private String typeQueue;

  @Column(name = "processing")
  private Boolean processing;

  @CreatedDate
  @Column(name = "CREATED_DATE")
  private OffsetDateTime createdDate;

  @LastModifiedDate
  @Column(name = "LAST_MODIFIED_DATE")
  private OffsetDateTime lastModifiedDate;

  public void setTypeQueue(VacancyAnalysisQueueType typeQueue) {
    this.typeQueue = typeQueue == null ? null : typeQueue.getId();
  }

  public VacancyAnalysisQueueType getTypeQueue() {
    return typeQueue == null ? null : VacancyAnalysisQueueType.fromId(typeQueue);
  }
}