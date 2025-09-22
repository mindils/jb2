package ru.mindils.jb2.app.repository;

import io.jmix.core.DataManager;
import org.springframework.stereotype.Repository;
import ru.mindils.jb2.app.entity.LLMModel;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class LLMModelRepository {

  private final DataManager dataManager;

  public LLMModelRepository(DataManager dataManager) {
    this.dataManager = dataManager;
  }

  public List<LLMModel> findAllAvailableOrderedByPriority() {
    return dataManager.load(LLMModel.class)
        .query("select m from jb2_LLMModel m where m.enabled = true " +
            "and (m.availableAfter is null or m.availableAfter <= current_timestamp) " +
            "order by m.priorityOrder asc, m.id asc")
        .list();
  }

  public Optional<LLMModel> findDefault() {
    return dataManager.load(LLMModel.class)
        .query("select m from jb2_LLMModel m where m.isDefault = true and m.enabled = true")
        .optional();
  }

  public Optional<LLMModel> findByModelName(String modelName) {
    return dataManager.load(LLMModel.class)
        .query("select m from jb2_LLMModel m where m.modelName = :modelName")
        .parameter("modelName", modelName)
        .optional();
  }

  public void recordFailure(LLMModel model) {
    model.setFailureCount((model.getFailureCount() == null ? 0 : model.getFailureCount()) + 1);
    model.setLastFailure(OffsetDateTime.now());

    // Устанавливаем время простоя в зависимости от количества ошибок
    OffsetDateTime availableAfter = calculateAvailableAfter(model.getFailureCount());
    model.setAvailableAfter(availableAfter);

    dataManager.save(model);
  }

  public void recordSuccess(LLMModel model) {
    model.setFailureCount(0);
    model.setLastSuccess(OffsetDateTime.now());
    model.setAvailableAfter(null); // Убираем ограничения
    dataManager.save(model);
  }

  private OffsetDateTime calculateAvailableAfter(int failureCount) {
    // Прогрессивное увеличение времени простоя
    return switch (failureCount) {
      case 1 -> OffsetDateTime.now().plusMinutes(1);   // 1 минута
      case 2 -> OffsetDateTime.now().plusMinutes(5);   // 5 минут
      case 3 -> OffsetDateTime.now().plusMinutes(15);  // 15 минут
      case 4 -> OffsetDateTime.now().plusHours(1);     // 1 час
      default -> OffsetDateTime.now().plusHours(4);    // 4 часа для 5+ ошибок
    };
  }

  public void resetFailureCount(Long modelId) {
    LLMModel model = dataManager.load(LLMModel.class).id(modelId).one();
    model.setFailureCount(0);
    model.setLastFailure(null);
    model.setAvailableAfter(null); // Убираем ограничения
    dataManager.save(model);
  }

  public LLMModel save(LLMModel model) {
    return dataManager.save(model);
  }

  public List<LLMModel> findAll() {
    return dataManager.load(LLMModel.class)
        .query("select m from jb2_LLMModel m order by m.priorityOrder asc, m.name asc")
        .list();
  }
}