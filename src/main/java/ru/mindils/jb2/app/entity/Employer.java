package ru.mindils.jb2.app.entity;

import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;


@JmixEntity
@Table(name = "JB2_EMPLOYER")
@Entity(name = "jb2_Employer")
public class Employer {

  @Id
  @Column(name = "id", nullable = false)
  private String id;

  @InstanceName
  @Column(name = "name")
  private String name;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

}