package ru.mindils.jb2.app.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jmix.core.DataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;
import ru.mindils.jb2.app.entity.Vacancy;
import ru.mindils.jb2.app.entity.VacancyLlmAnalysis;
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

  private final ResilientLLMService llmService;
  private final Map<VacancyLlmAnalysisType, PromptGenerator> promptGenerators;
  private final DataManager dataManager;
  private final ObjectMapper objectMapper;
  private final UuidGenerator uuidGenerator;

  public VacancyLlmAnalysisService(ResilientLLMService llmService,
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
   * @return результат анализа в виде JSON
   */
  public String analyze(Vacancy vacancy, VacancyLlmAnalysisType analysisType) {
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

      // Вызываем LLM
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
  public String analyze(Vacancy vacancy, String analysisTypeCode) {
    VacancyLlmAnalysisType analysisType = VacancyLlmAnalysisType.fromId(analysisTypeCode);
    return analyze(vacancy, analysisType);
  }

  /**
   * Сохранение результата анализа в базу данных
   * Использует детерминированный UUID, поэтому может обновлять существующие записи
   *
   * @param analysisType тип анализа
   * @param vacancyId    ID вакансии
   * @param result       результат анализа в виде строки
   * @return Optional с ошибкой, если JSON не удалось распарсить, иначе empty
   */
  public Optional<String> saveAnalysisResult(String vacancyId,
                                             VacancyLlmAnalysisType analysisType,
                                             String result) {
    log.info("Saving analysis result for vacancy {} with type {}", vacancyId, analysisType);

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

      // Обновляем данные
      analysis.setAnalyzeDataString(result);

      // Пытаемся распарсить JSON
      String jsonParseError = null;
      try {
        JsonNode jsonNode = objectMapper.readTree(result);
        // Если успешно распарсили, сохраняем и в JSON поле
        analysis.setAnalyzeData(jsonNode);
        log.debug("Successfully parsed JSON for vacancy {} analysis type {}", vacancyId, analysisType);

      } catch (JsonProcessingException e) {
        jsonParseError = "Failed to parse JSON: " + e.getMessage();
        log.warn("Failed to parse JSON for vacancy {} analysis type {}: {}",
            vacancyId, analysisType, e.getMessage());
        // analyzeData будет null для обновления, или останется старое значение
        if (!isUpdate) {
          analysis.setAnalyzeData(null);
        }
      }

      // Сохраняем в базу данных
      VacancyLlmAnalysis saved = dataManager.save(analysis);

      if (isUpdate) {
        log.info("Successfully updated analysis result with UUID {} for vacancy {} type {}",
            saved.getId(), vacancyId, analysisType);
      } else {
        log.info("Successfully created analysis result with UUID {} for vacancy {} type {}",
            saved.getId(), vacancyId, analysisType);
      }

      // Возвращаем ошибку парсинга JSON если была
      return Optional.ofNullable(jsonParseError);

    } catch (Exception e) {
      log.error("Error saving analysis result for vacancy {} type {}: {}",
          vacancyId, analysisType, e.getMessage(), e);
      throw new RuntimeException("Failed to save analysis result", e);
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
