package ru.mindils.jb2.app.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jmix.core.DataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;
import ru.mindils.jb2.app.dto.LlmAnalysisResponse;
import ru.mindils.jb2.app.entity.Vacancy;
import ru.mindils.jb2.app.entity.VacancyLlmAnalysis;
import ru.mindils.jb2.app.entity.VacancyLlmAnalysisStatus;
import ru.mindils.jb2.app.entity.VacancyLlmAnalysisType;
import ru.mindils.jb2.app.service.analysis.prompt.PromptGenerator;
import ru.mindils.jb2.app.util.UuidGenerator;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class VacancyLlmAnalysisService {

  private static final Logger log = LoggerFactory.getLogger(VacancyLlmAnalysisService.class);

  private final SimpleLLMService llmService;
  private final Map<VacancyLlmAnalysisType, PromptGenerator> promptGenerators;
  private final DataManager dataManager;
  private final ObjectMapper objectMapper;
  private final UuidGenerator uuidGenerator;

  public VacancyLlmAnalysisService(SimpleLLMService llmService,
                                   List<PromptGenerator> promptGenerators, DataManager dataManager, ObjectMapper objectMapper, UuidGenerator uuidGenerator) {
    this.llmService = llmService;
    this.promptGenerators = promptGenerators.stream()
        .collect(Collectors.toMap(
            PromptGenerator::getSupportedType,
            Function.identity()
        ));
    this.dataManager = dataManager;
    this.objectMapper = objectMapper;
    this.uuidGenerator = uuidGenerator;
  }

  /**
   * Анализ вакансии по указанному типу
   *
   * @param vacancy      вакансия для анализа
   * @param analysisType тип анализа
   * @return результат анализа в виде DTO с распарсенным JSON
   */
  public LlmAnalysisResponse analyze(Vacancy vacancy, VacancyLlmAnalysisType analysisType) {
    log.info("Starting analysis for vacancy {} with type {}", vacancy.getId(), analysisType);

    try {
      // Получаем генератор промпта для указанного типа
      PromptGenerator promptGenerator = promptGenerators.get(analysisType);
      if (promptGenerator == null) {
        throw new IllegalArgumentException("No prompt generator found for analysis type: " + analysisType);
      }

      // Генерируем промпт
      String prompt = promptGenerator.generatePrompt(vacancy);
      log.debug("Generated prompt for analysis type {}: {}", analysisType, prompt);

      // Вызываем LLM и получаем DTO
      return llmService.callLLM(prompt, getOptionsForAnalysisType(analysisType));

    } catch (Exception e) {
      log.error("Error during analysis for vacancy {} with type {}: {}",
          vacancy.getId(), analysisType, e.getMessage(), e);
      throw new RuntimeException("Analysis failed", e);
    }
  }

  /**
   * Анализ вакансии по строковому коду типа
   */
  public LlmAnalysisResponse analyze(Vacancy vacancy, String analysisTypeCode) {
    VacancyLlmAnalysisType analysisType = VacancyLlmAnalysisType.fromId(analysisTypeCode);
    return analyze(vacancy, analysisType);
  }

  /**
   * Сохранение результата анализа в базу данных из DTO
   * Использует детерминированный UUID, поэтому может обновлять существующие записи
   *
   * @param vacancyId    ID вакансии
   * @param analysisType тип анализа
   * @param llmResponse  DTO с результатом анализа
   * @return Optional с ошибкой парсинга JSON, если была, иначе empty
   */
  public void saveAnalysisResult(String vacancyId,
                                 VacancyLlmAnalysisType analysisType,
                                 LlmAnalysisResponse llmResponse) {
    log.info("Saving analysis result for vacancy {} with type {} from LLM call {}",
        vacancyId, analysisType, llmResponse.llmCallId());

    try {
      // Генерируем детерминированный UUID для этой комбинации вакансии и типа анализа
      UUID analysisId = uuidGenerator.generateUuid(vacancyId, analysisType.getId());
      log.debug("Generated UUID {} for vacancy {} analysis type {}", analysisId, vacancyId, analysisType);

      // Загружаем вакансию
      Vacancy vacancy = dataManager.load(Vacancy.class).id(vacancyId).one();

      // Пытаемся найти существующую запись
      Optional<VacancyLlmAnalysis> existingAnalysis = dataManager.load(VacancyLlmAnalysis.class)
          .id(analysisId)
          .optional();

      VacancyLlmAnalysis analysis;
      boolean isUpdate = false;

      if (existingAnalysis.isPresent()) {
        // Обновляем существующую запись
        analysis = existingAnalysis.get();
        isUpdate = true;
        log.debug("Found existing analysis record with UUID {}, updating", analysisId);
      } else {
        // Создаем новую запись
        analysis = dataManager.create(VacancyLlmAnalysis.class);
        analysis.setId(analysisId);
        analysis.setVacancy(vacancy);
        analysis.setAnalyzeType(analysisType.getId());
        log.debug("Creating new analysis record with UUID {}", analysisId);
      }

      // Сохраняем данные из DTO
      analysis.setAnalyzeDataString(llmResponse.rawResponse());
      analysis.setStatus(VacancyLlmAnalysisStatus.DONE);
      analysis.setLlmCallLogId(llmResponse.llmCallId());
      analysis.setLlmModel(llmResponse.llmModel());

      // Устанавливаем JSON данные и ID лога LLM
      if (llmResponse.hasValidJson()) {
        analysis.setAnalyzeData(llmResponse.jsonNode());
        log.debug("Successfully set JSON data for vacancy {} analysis type {}", vacancyId, analysisType);
      } else {
        // Если JSON не распарсился, оставляем поле пустым
        if (!isUpdate) {
          analysis.setAnalyzeData(null);
        }
      }

      // Сохраняем ID вызова LLM для трейсабилити (если есть такое поле)
      // analysis.setLlmCallId(llmResponse.getLlmCallId());

      // Сохраняем в базу данных
      VacancyLlmAnalysis saved = dataManager.save(analysis);

      if (isUpdate) {
        log.info("Successfully updated analysis result with UUID {} for vacancy {} type {} from LLM call {}",
            saved.getId(), vacancyId, analysisType, llmResponse.llmCallId());
      } else {
        log.info("Successfully created analysis result with UUID {} for vacancy {} type {} from LLM call {}",
            saved.getId(), vacancyId, analysisType, llmResponse.llmCallId());
      }
    } catch (Exception e) {
      log.error("Error saving analysis result for vacancy {} type {} from LLM call {}: {}",
          vacancyId, analysisType, llmResponse.llmCallId(), e.getMessage(), e);
      throw new RuntimeException("Failed to save analysis result", e);
    }
  }

  /**
   * Сохранение записи анализа с указанным статусом (без данных анализа)
   * Использует детерминированный UUID, поэтому может обновлять существующие записи
   *
   * @param vacancyId    ID вакансии
   * @param analysisType тип анализа
   * @param status       статус анализа (SKIP, ERROR, и т.д.)
   */
  public void saveAnalysisStatus(String vacancyId, VacancyLlmAnalysisType analysisType, VacancyLlmAnalysisStatus status) {
    log.info("Setting analysis status {} for vacancy {} with type {}", status, vacancyId, analysisType);

    try {
      // Генерируем детерминированный UUID для этой комбинации вакансии и типа анализа
      UUID analysisId = uuidGenerator.generateUuid(vacancyId, analysisType.getId());
      log.debug("Generated UUID {} for vacancy {} analysis type {}", analysisId, vacancyId, analysisType);

      // Загружаем вакансию
      Vacancy vacancy = dataManager.load(Vacancy.class).id(vacancyId).one();

      // Пытаемся найти существующую запись
      Optional<VacancyLlmAnalysis> existingAnalysis = dataManager.load(VacancyLlmAnalysis.class)
          .id(analysisId)
          .optional();

      VacancyLlmAnalysis analysis;
      boolean isUpdate = false;

      if (existingAnalysis.isPresent()) {
        // Обновляем существующую запись
        analysis = existingAnalysis.get();
        isUpdate = true;
        log.debug("Found existing analysis record with UUID {}, updating to {} status", analysisId, status);
      } else {
        // Создаем новую запись
        analysis = dataManager.create(VacancyLlmAnalysis.class);
        analysis.setId(analysisId);
        analysis.setVacancy(vacancy);
        analysis.setAnalyzeType(analysisType.getId());
        log.debug("Creating new analysis record with UUID {} and {} status", analysisId, status);
      }

      // Устанавливаем переданный статус, остальные поля оставляем пустыми
      analysis.setStatus(status);

      // Очищаем данные анализа (если это обновление существующей записи)
      analysis.setAnalyzeDataString(null);
      analysis.setAnalyzeData(null);

      // Сохраняем в базу данных
      VacancyLlmAnalysis saved = dataManager.save(analysis);

      if (isUpdate) {
        log.info("Successfully updated analysis record with UUID {} for vacancy {} type {} to {} status",
            saved.getId(), vacancyId, analysisType, status);
      } else {
        log.info("Successfully created analysis record with UUID {} for vacancy {} type {} with {} status",
            saved.getId(), vacancyId, analysisType, status);
      }

    } catch (Exception e) {
      log.error("Error setting analysis status {} for vacancy {} type {}: {}",
          status, vacancyId, analysisType, e.getMessage(), e);
      throw new RuntimeException("Failed to set analysis status", e);
    }
  }

  /**
   * Настройки LLM в зависимости от типа анализа
   */
  private OpenAiChatOptions getOptionsForAnalysisType(VacancyLlmAnalysisType analysisType) {
    return switch (analysisType) {
      case JAVA_PRIMARY -> OpenAiChatOptions.builder()
          .temperature(0.0)
          .maxTokens(300)
          .build();
      default -> OpenAiChatOptions.builder()
          .temperature(0.0)
          .maxTokens(300)
          .build();
    };
  }
}
