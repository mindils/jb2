package ru.mindils.jb2.app.service;

import com.fasterxml.jackson.databind.JsonNode;
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

  public void saveAnalysisStatus(String vacancyId, VacancyLlmAnalysisType analysisType, VacancyLlmAnalysisStatus status) {
    saveAnalysisStatus(vacancyId, analysisType, status, null);
  }

  /**
   * Сохранение записи анализа с указанным статусом (без данных анализа)
   * Использует детерминированный UUID, поэтому может обновлять существующие записи
   *
   * @param vacancyId    ID вакансии
   * @param analysisType тип анализа
   * @param status       статус анализа (SKIP, ERROR, и т.д.)
   */
  public void saveAnalysisStatus(String vacancyId, VacancyLlmAnalysisType analysisType, VacancyLlmAnalysisStatus status, String message) {
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
      analysis.setMessage(message);

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
   * Проверяет, существует ли уже завершенный анализ для данной вакансии и типа
   * Проверяет только наличие готового JSON - если его нет, значит была ошибка парсинга
   */
  public boolean hasExistingAnalysis(String vacancyId, VacancyLlmAnalysisType analysisType) {
    log.debug("Checking existing analysis for vacancy {} with type {}", vacancyId, analysisType);

    try {
      // Генерируем детерминированный UUID для поиска
      UUID analysisId = uuidGenerator.generateUuid(vacancyId, analysisType.getId());

      // Проверяем существование записи со статусом DONE и готовым JSON
      Optional<VacancyLlmAnalysis> existingAnalysis = dataManager
          .load(VacancyLlmAnalysis.class)
          .id(analysisId)
          .optional();

      boolean exists = existingAnalysis.isPresent() &&
          existingAnalysis.get().getStatus() == VacancyLlmAnalysisStatus.DONE &&
          existingAnalysis.get().getAnalyzeData() != null;

      log.debug("Existing analysis for vacancy {} type {}: {}", vacancyId, analysisType, exists);
      return exists;

    } catch (Exception e) {
      log.error("Error checking existing analysis for vacancy {} type {}: {}",
          vacancyId, analysisType, e.getMessage(), e);
      return false;
    }
  }

  /**
   * Получает существующий результат анализа и конвертирует в DTO
   * Требует наличие готового JSON - если его нет, считаем что нужна новая генерация
   */
  public LlmAnalysisResponse getExistingAnalysis(String vacancyId, VacancyLlmAnalysisType analysisType) {
    log.info("Getting existing analysis for vacancy {} with type {}", vacancyId, analysisType);

    try {
      // Генерируем детерминированный UUID для поиска
      UUID analysisId = uuidGenerator.generateUuid(vacancyId, analysisType.getId());

      // Загружаем существующую запись
      Optional<VacancyLlmAnalysis> existingAnalysis = dataManager
          .load(VacancyLlmAnalysis.class)
          .id(analysisId)
          .optional();

      if (existingAnalysis.isEmpty()) {
        throw new RuntimeException("No existing analysis found for vacancy " + vacancyId + " type " + analysisType);
      }

      VacancyLlmAnalysis analysis = existingAnalysis.get();

      if (analysis.getStatus() != VacancyLlmAnalysisStatus.DONE) {
        throw new RuntimeException("Existing analysis is not completed for vacancy " + vacancyId + " type " + analysisType +
            ", status: " + analysis.getStatus());
      }

      // Получаем данные из записи
      String rawResponse = analysis.getAnalyzeDataString();
      JsonNode jsonNode = analysis.getAnalyzeData();
      Long llmCallId = analysis.getLlmCallLogId();
      String llmModel = analysis.getLlmModel();

      // Требуем наличие готового JSON
      if (jsonNode != null && rawResponse != null) {
        log.info("Successfully retrieved existing analysis for vacancy {} type {} from LLM call {}",
            vacancyId, analysisType, llmCallId);
        return LlmAnalysisResponse.success(rawResponse, jsonNode, llmCallId, llmModel);
      }

      // Если готового JSON нет - считаем что была ошибка парсинга, нужна новая генерация
      throw new RuntimeException("Existing analysis has no valid JSON data for vacancy " + vacancyId + " type " + analysisType +
          " - previous parsing failed, need fresh analysis");

    } catch (Exception e) {
      log.error("Error getting existing analysis for vacancy {} type {}: {}",
          vacancyId, analysisType, e.getMessage(), e);
      throw new RuntimeException("Failed to get existing analysis", e);
    }
  }

  /**
   * Настройки LLM в зависимости от типа анализа
   * Лимиты токенов основаны на сложности промпта и ожидаемой длине JSON-ответа
   */
  private OpenAiChatOptions getOptionsForAnalysisType(VacancyLlmAnalysisType analysisType) {
    return switch (analysisType) {
      // Простые короткие ответы (1 boolean) - 2000 токенов
      case JAVA_PRIMARY -> OpenAiChatOptions.builder()
          .temperature(0.0)
          .maxTokens(2000)
          .build();

      // Простые ответы (3-5 полей) - 3000 токенов
      case COMPENSATION, STOP_FACTORS -> OpenAiChatOptions.builder()
          .temperature(0.0)
          .maxTokens(3000)
          .build();

      // Средние ответы (5-8 полей, умеренная сложность) - 4000 токенов
      case BENEFITS, TECHNICAL -> OpenAiChatOptions.builder()
          .temperature(0.0)
          .maxTokens(4000)
          .build();

      // Сложные ответы (детальный анализ, длинные правила) - 5000 токенов
      case WORK_CONDITIONS -> OpenAiChatOptions.builder()
          .temperature(0.0)
          .maxTokens(5000)
          .build();

      // Самые сложные ответы (огромные промпты, множественные направления) - 6000 токенов
      case EQUIPMENT, INDUSTRY -> OpenAiChatOptions.builder()
          .temperature(0.0)
          .maxTokens(6000)
          .build();

      // По умолчанию - 4000 токенов (безопасное значение)
      default -> OpenAiChatOptions.builder()
          .temperature(0.0)
          .maxTokens(4000)
          .build();
    };
  }
}