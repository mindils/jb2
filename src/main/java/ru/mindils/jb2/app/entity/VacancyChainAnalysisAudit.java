package ru.mindils.jb2.app.entity;

import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@JmixEntity
@Table(name = "JB2_VACANCY_CHAIN_ANALYSIS_AUDIT", indexes = {
    @Index(name = "IDX_CHAIN_AUDIT_VACANCY", columnList = "vacancy_id"),
    @Index(name = "IDX_CHAIN_AUDIT_CHAIN_ID", columnList = "CHAIN_ID"),
    @Index(name = "IDX_CHAIN_AUDIT_EXECUTION_TIME", columnList = "execution_time"),
    @Index(name = "IDX_CHAIN_AUDIT_SUCCESS", columnList = "success")
})
@Getter
@Setter
@Entity(name = "jb2_VacancyChainAnalysisAudit")
public class VacancyChainAnalysisAudit {

  @Id
  @Column(name = "id", nullable = false)
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "vacancy_id", nullable = false)
  private String vacancyId;

  @InstanceName
  @Column(name = "CHAIN_ID")
  private String chainId;

  @Column(name = "success", nullable = false)
  private Boolean success = false;

  @Column(name = "execution_time", nullable = false)
  private LocalDateTime executionTime;

  @Column(name = "error_message", length = 1000)
  private String errorMessage;

  @Column(name = "stopped_at")
  private String stoppedAt;

  @Column(name = "stop_reason", length = 500)
  private String stopReason;

  @Column(name = "steps_executed")
  private Integer stepsExecuted;

  @Column(name = "final_score")
  private Integer finalScore;

  @Column(name = "rating")
  private String rating;

  @Column(name = "summary", length = 500)
  private String summary;

  @CreatedDate
  @Column(name = "created_date")
  private LocalDateTime createdDate;

  public ChainAnalysisType getChainAnalysisType() {
    return chainId == null ? null : ChainAnalysisType.fromId(chainId);
  }

  public void setChainId(ChainAnalysisType chainId) {
    this.chainId = chainId == null ? null : chainId.getId();
  }
}