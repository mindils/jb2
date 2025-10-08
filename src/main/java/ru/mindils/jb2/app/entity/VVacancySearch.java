package ru.mindils.jb2.app.entity;

import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import io.jmix.data.DbView;
import io.jmix.data.DdlGeneration;
import jakarta.persistence.*;

@DbView
@DdlGeneration(unmappedColumns = {"salary", "professional_roles", "work_format"})
@JmixEntity
@Table(name = "v_vacancy_search")
@Entity(name = "jb2_VVacancySearch")
public class VVacancySearch {
  @Column(name = "id")
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

  public Integer getScore() {
    return score;
  }

  public void setScore(Integer score) {
    this.score = score;
  }

  public String getRating() {
    return rating;
  }

  public void setRating(String rating) {
    this.rating = rating;
  }

  public String getPositiveDescription() {
    return positiveDescription;
  }

  public void setPositiveDescription(String positiveDescription) {
    this.positiveDescription = positiveDescription;
  }

  public String getNegativeDescription() {
    return negativeDescription;
  }

  public void setNegativeDescription(String negativeDescription) {
    this.negativeDescription = negativeDescription;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getMyStatus() {
    return myStatus;
  }

  public void setMyStatus(String myStatus) {
    this.myStatus = myStatus;
  }

  public Boolean getIsJavaVacancy() {
    return isJavaVacancy;
  }

  public void setIsJavaVacancy(Boolean isJavaVacancy) {
    this.isJavaVacancy = isJavaVacancy;
  }

  public String getEmployerName() {
    return employerName;
  }

  public void setEmployerName(String employerName) {
    this.employerName = employerName;
  }

  public Boolean getArchived() {
    return archived;
  }

  public void setArchived(Boolean archived) {
    this.archived = archived;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

}