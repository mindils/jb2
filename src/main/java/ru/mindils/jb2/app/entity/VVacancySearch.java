package ru.mindils.jb2.app.entity;

import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import io.jmix.data.DbView;
import io.jmix.data.DdlGeneration;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@DbView
@DdlGeneration(unmappedColumns = {"salary", "professional_roles", "work_format"})
@JmixEntity
@Table(name = "v_vacancy_search")
@Entity(name = "jb2_VVacancySearch")
@Getter
@Setter
public class VVacancySearch {
  @Column(name = "id", nullable = false)
  @Id
  private String id;

  @Column(name = "archived")
  private Boolean archived;

  @Column(name = "employer_name")
  private String employerName;

  @Column(name = "is_java_vacancy")
  private Boolean isJavaVacancy;

  @Column(name = "my_status")
  private String myStatus;

  @InstanceName
  @Column(name = "name")
  private String name;

  @Column(name = "negative_description")
  @Lob
  private String negativeDescription;

  @Column(name = "positive_description")
  @Lob
  private String positiveDescription;

  @Column(name = "rating", length = 50)
  private String rating;

  @Column(name = "score")
  private Integer score;

  @Column(name = "MY_STATUS_EMPLOYER")
  private String myStatusEmployer;

  public EmployerStatus getMyStatusEmployer() {
    return myStatusEmployer == null ? null : EmployerStatus.fromId(myStatusEmployer);
  }

  public void setMyStatusEmployer(EmployerStatus myStatusEmployer) {
    this.myStatusEmployer = myStatusEmployer == null ? null : myStatusEmployer.getId();
  }

  public VacancyScoreRating getRating() {
    return rating == null ? null : VacancyScoreRating.fromId(rating);
  }

  public void setRating(VacancyScoreRating rating) {
    this.rating = rating ==  null ? null : rating.getId();
  }

  public VacancyStatus getMyStatus() {
    return myStatus ==  null ? null : VacancyStatus.fromId(myStatus);
  }

  public void setMyStatus(VacancyStatus myStatus) {
    this.myStatus = myStatus == null ? null : myStatus.getId();
  }
}