package ru.mindils.jb2.app.service.analysis.chain;

import io.jmix.core.DataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mindils.jb2.app.entity.VacancyChainAnalysisAudit;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Сервис для аудита выполнения цепочек анализа
 */
@Service
public class ChainAnalysisAuditService {

  private static final Logger log = LoggerFactory.getLogger(ChainAnalysisAuditService.class);

  private final DataManager dataManager;
  private final ChainAnalysisResultUtils resultUtils;

  public ChainAnalysisAuditService(DataManager dataManager, ChainAnalysisResultUtils resultUtils) {
    this.dataManager = dataManager;
    this.resultUtils = resultUtils;
  }

  /**
   * Записать результат анализа в аудит
   */
  @Transactional
  public void auditAnalysisResult(ChainAnalysisResult result) {
    try {
      VacancyChainAnalysisAudit audit = dataManager.create(VacancyChainAnalysisAudit.class);

      audit.setVacancyId(result.vacancyId());
      audit.setChainId(result.chainConfig().chainId());
      audit.setSuccess(result.success());
      audit.setExecutionTime(LocalDateTime.now());

      if (result.errorMessage() != null) {
        audit.setErrorMessage(truncate(result.errorMessage(), 1000));
      }

      audit.setStoppedAt(result.stoppedAt());
      audit.setStopReason(result.stopReason());
      audit.setStepsExecuted(result.stepResults().size());
      audit.setSummary(truncate(resultUtils.createSummaryText(result), 500));

      if (result.finalScore() != null) {
        audit.setFinalScore(result.finalScore().totalScore());
        audit.setRating(result.finalScore().rating().toString());
      }

      dataManager.save(audit);

      log.debug("Записан результат аудита для вакансии {} (цепочка: {})",
          result.vacancyId(), result.chainConfig().chainId());

    } catch (Exception e) {
      log.error("Ошибка записи аудита для вакансии {}: {}",
          result.vacancyId(), e.getMessage(), e);
    }
  }

  /**
   * Получить статистику выполнения цепочек
   */
  public ChainAnalysisStats getAnalysisStats(String chainId, LocalDateTime from, LocalDateTime to) {
    List<VacancyChainAnalysisAudit> audits = dataManager.load(VacancyChainAnalysisAudit.class)
        .query("select e from jb2_VacancyChainAnalysisAudit e " +
            "where (:chainId is null or e.chainId = :chainId) " +
            "and e.executionTime between :from and :to")
        .parameter("chainId", chainId)
        .parameter("from", from)
        .parameter("to", to)
        .list();

    int total = audits.size();
    int successful = (int) audits.stream().filter(VacancyChainAnalysisAudit::getSuccess).count();
    int failed = total - successful;
    int stoppedEarly = (int) audits.stream().filter(a -> a.getStoppedAt() != null).count();

    double avgScore = audits.stream()
        .filter(a -> a.getFinalScore() != null)
        .mapToInt(VacancyChainAnalysisAudit::getFinalScore)
        .average()
        .orElse(0.0);

    return new ChainAnalysisStats(total, successful, failed, stoppedEarly, avgScore);
  }

  private String truncate(String text, int maxLength) {
    if (text == null || text.length() <= maxLength) return text;
    return text.substring(0, maxLength - 3) + "...";
  }

  /**
   * Статистика выполнения цепочек анализа
   */
  public static record ChainAnalysisStats(
      int total,
      int successful,
      int failed,
      int stoppedEarly,
      double avgScore
  ) {}
}