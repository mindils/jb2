package ru.mindils.jb2.app.temporal.workflow;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface MasterVacancyProcessingWorkflow {

  /**
   * Запускает полный цикл обработки вакансий:
   * 1. Добавление Java вакансий в очередь на обновление
   * 2. Обработка очереди обновления
   * 3. Синхронизация недавних вакансий
   * 4. Добавление вакансий на полный анализ
   * 5. Обработка полного анализа
   * 6. Расчет оценок вакансий
   *
   * @param syncDays количество дней для синхронизации (обычно 1)
   */
  @WorkflowMethod
  void processAllVacancies(int syncDays);
}