package ru.mindils.jb2.app.entity;

import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;

import java.time.OffsetDateTime;

@JmixEntity
@Table(name = "JB2_LLM_CALL_LOG", indexes = {
    @Index(name = "IDX_LLM_CALL_LOG_MODEL", columnList = "MODEL_NAME"),
    @Index(name = "IDX_LLM_CALL_LOG_STATUS", columnList = "SUCCESS"),
    @Index(name = "IDX_LLM_CALL_LOG_DATE", columnList = "CREATED_DATE"),
    @Index(name = "IDX_LLM_CALL_LOG_REQUEST_ID", columnList = "REQUEST_ID")
})
@Entity(name = "jb2_LlmCallLog")
@Getter
@Setter
public class LlmCallLog {
  @Column(name = "ID", nullable = false)
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // Идентификатор запроса для связи retry попыток
  @Column(name = "REQUEST_ID")
  private String requestId;

  // Информация о модели
  @Column(name = "MODEL_NAME", nullable = false)
  private String modelName;

  @Column(name = "MODEL_ID")
  private Long modelId;

  @Column(name = "BASE_URL")
  private String baseUrl;

  // Запрос
  @Lob
  @Column(name = "PROMPT")
  private String prompt;

  @Column(name = "PROMPT_LENGTH")
  private Integer promptLength;

  // Параметры запроса
  @Column(name = "TEMPERATURE")
  private Double temperature;

  @Column(name = "MAX_TOKENS")
  private Integer maxTokens;

  // Ответ
  @Lob
  @Column(name = "RESPONSE")
  private String response;

  @Column(name = "RESPONSE_LENGTH")
  private Integer responseLength;

  // Статус и ошибки
  @Column(name = "SUCCESS", nullable = false)
  private Boolean success = false;

  @Column(name = "HTTP_STATUS_CODE")
  private Integer httpStatusCode;

  @Column(name = "ERROR_TYPE")
  private String errorType; // RATE_LIMIT, QUOTA_EXCEEDED, TIMEOUT, API_ERROR, UNKNOWN

  @Lob
  @Column(name = "ERROR_MESSAGE")
  private String errorMessage;

  @Column(name = "ERROR_DETAILS")
  private String errorDetails;

  // Производительность
  @Column(name = "DURATION_MS")
  private Long durationMs;

  @Column(name = "RETRY_COUNT")
  private Integer retryCount = 0;

  @CreatedDate
  @Column(name = "CREATED_DATE")
  private OffsetDateTime createdDate;

  // Вспомогательные методы для быстрого создания логов
  public static LlmCallLog startCall(String requestId, LLMModel model, String prompt,
                                     Double temperature, Integer maxTokens) {
    LlmCallLog log = new LlmCallLog();
    log.setRequestId(requestId);
    log.setModelId(model.getId());
    log.setModelName(model.getModelName());
    log.setBaseUrl(model.getBaseUrl());
    log.setPrompt(prompt);
    log.setPromptLength(prompt != null ? prompt.length() : 0);
    log.setTemperature(temperature);
    log.setMaxTokens(maxTokens);
    log.setCreatedDate(OffsetDateTime.now());
    return log;
  }

  public void completeWithSuccess(String response, long durationMs) {
    this.success = true;
    this.response = response;
    this.responseLength = response != null ? response.length() : 0;
    this.durationMs = durationMs;
    this.httpStatusCode = 200;
  }

  public void completeWithError(Exception e, Integer httpStatus, String errorType, long durationMs) {
    this.success = false;
    this.httpStatusCode = httpStatus;
    this.errorType = errorType;
    this.errorMessage = e.getMessage();
    this.errorDetails = e.getClass().getName();
    this.durationMs = durationMs;
  }

}