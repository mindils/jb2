package ru.mindils.jb2.app.entity;

import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.OffsetDateTime;
import java.util.UUID;

@JmixEntity
@Table(name = "jb2_vacancy_score", indexes = {
    @Index(name = "idx_vacancy_score_vacancy", columnList = "vacancy_id"),
    @Index(name = "idx_vacancy_score_rating", columnList = "rating")
})
@Entity(name = "jb2_VacancyScore")
@Getter
@Setter
public class VacancyScore {

  @Id
  @Column(name = "uid", nullable = false)
  private UUID id;

  @JoinColumn(name = "vacancy_id", nullable = false)
  @OneToOne(fetch = FetchType.LAZY)
  private Vacancy vacancy;

  @Column(name = "total_score", nullable = false)
  private Integer totalScore;

  @Column(name = "rating", nullable = false, length = 50)
  private String rating;

  @Lob
  @Column(name = "positive_description", columnDefinition = "text")
  private String positiveDescription;

  @Lob
  @Column(name = "negative_description", columnDefinition = "text")
  private String negativeDescription;

  @Column(name = "version")
  private Integer version = 1; // Версия алгоритма расчета

  @CreatedDate
  @Column(name = "created_date")
  private OffsetDateTime createdDate;

  @LastModifiedDate
  @Column(name = "last_modified_date")
  private OffsetDateTime lastModifiedDate;

  public VacancyScoreRating getRating() {
    return rating == null ? null : VacancyScoreRating.fromId(rating);
  }

  public void setRating(VacancyScoreRating rating) {
    this.rating = rating == null ? null : rating.getId();
  }
}