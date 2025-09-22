package ru.mindils.jb2.app.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.mindils.jb2.app.entity.LLMModel;
import ru.mindils.jb2.app.entity.LLMModelStats;
import ru.mindils.jb2.app.repository.LLMModelRepository;

import java.util.List;

@Service
public class LLMManagementService {

  private static final Logger log = LoggerFactory.getLogger(LLMManagementService.class);

  private final LLMModelRepository llmModelRepository;
  private final ResilientLLMService resilientLLMService;

  public LLMManagementService(LLMModelRepository llmModelRepository,
                              ResilientLLMService resilientLLMService) {
    this.llmModelRepository = llmModelRepository;
    this.resilientLLMService = resilientLLMService;
  }

  /**
   * Получить статистику по всем моделям
   */
  public List<LLMModelStats> getModelsStats() {
    return llmModelRepository.findAll().stream()
        .map(LLMModelStats::new)
        .toList();
  }

  /**
   * Сбросить счетчик ошибок для модели
   */
  public void resetModelFailures(Long modelId) {
    llmModelRepository.resetFailureCount(modelId);
    resilientLLMService.resetModelFailures(modelId);
    log.info("Reset failure count for model ID: {}", modelId);
  }

  /**
   * Сбросить счетчики ошибок для всех моделей
   */
  public void resetAllModelFailures() {
    List<LLMModel> models = llmModelRepository.findAll();
    for (LLMModel model : models) {
      resetModelFailures(model.getId());
    }
    log.info("Reset failure counts for all models");
  }

  /**
   * Включить/выключить модель
   */
  public void toggleModelEnabled(Long modelId, boolean enabled) {
    LLMModel model = llmModelRepository.findAll().stream()
        .filter(m -> m.getId().equals(modelId))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Model not found: " + modelId));

    model.setEnabled(enabled);
    llmModelRepository.save(model);

    log.info("{} model: {} ({})", enabled ? "Enabled" : "Disabled",
        model.getName(), model.getModelName());
  }

  /**
   * Установить модель как модель по умолчанию
   */
  public void setDefaultModel(Long modelId) {
    List<LLMModel> allModels = llmModelRepository.findAll();

    // Сбрасываем флаг default у всех моделей
    for (LLMModel model : allModels) {
      model.setIsDefault(false);
      llmModelRepository.save(model);
    }

    // Устанавливаем новую модель по умолчанию
    LLMModel newDefault = allModels.stream()
        .filter(m -> m.getId().equals(modelId))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Model not found: " + modelId));

    newDefault.setIsDefault(true);
    newDefault.setEnabled(true); // Автоматически включаем
    newDefault.setAvailableAfter(null); // Убираем ограничения
    llmModelRepository.save(newDefault);

    log.info("Set default model: {} ({})", newDefault.getName(), newDefault.getModelName());
  }

  /**
   * Обновить приоритет модели
   */
  public void updateModelPriority(Long modelId, int newPriority) {
    LLMModel model = llmModelRepository.findAll().stream()
        .filter(m -> m.getId().equals(modelId))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Model not found: " + modelId));

    model.setPriorityOrder(newPriority);
    llmModelRepository.save(model);

    log.info("Updated priority for model: {} to {}", model.getName(), newPriority);
  }

  /**
   * Создать новую модель
   */
  public LLMModel createModel(String name, String modelName, String baseUrl, String apiKey,
                              int priority, boolean enabled, boolean isDefault) {

    // Если устанавливаем как default, сбрасываем флаг у остальных
    if (isDefault) {
      List<LLMModel> allModels = llmModelRepository.findAll();
      for (LLMModel model : allModels) {
        if (model.getIsDefault()) {
          model.setIsDefault(false);
          llmModelRepository.save(model);
        }
      }
    }

    LLMModel newModel = new LLMModel();
    newModel.setName(name);
    newModel.setModelName(modelName);
    newModel.setBaseUrl(baseUrl);
    newModel.setApiKey(apiKey);
    newModel.setPriorityOrder(priority);
    newModel.setEnabled(enabled);
    newModel.setIsDefault(isDefault);
    newModel.setTemperature(0.1);
    newModel.setMaxTokens(1000);
    newModel.setTimeoutSeconds(60);

    return llmModelRepository.save(newModel);
  }
}