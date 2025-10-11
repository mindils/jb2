package ru.mindils.jb2.app.temporal.acrivity;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface MasterVacancyProcessingActivities {

    /**
     * Шаг 1: Добавляет в очередь все вакансии с java=true из v_vacancy_search
     * @return количество добавленных вакансий
     */
    @ActivityMethod
    int enqueueJavaVacanciesForUpdate();

    /**
     * Шаг 2: Запускает и дожидается завершения обработки очереди на обновление
     */
    @ActivityMethod
    void processUpdateQueue();

    /**
     * Шаг 3: Синхронизирует вакансии за последние N дней
     * @param days количество дней
     */
    @ActivityMethod
    void syncRecentVacancies(int days);

    /**
     * Шаг 4: Добавляет все вакансии на полный LLM анализ
     * @return количество добавленных вакансий
     */
    @ActivityMethod
    int enqueueForFullAnalysis();

    /**
     * Шаг 5: Запускает и дожидается завершения полного LLM анализа
     */
    @ActivityMethod
    void processFullAnalysisQueue();

    /**
     * Шаг 6: Рассчитывает оценки для Java вакансий
     * @return результат расчета (total, success, failed, skipped)
     */
    @ActivityMethod
    String calculateVacancyScores();

    /**
     * Проверяет, запущен ли указанный workflow
     */
    @ActivityMethod
    boolean isWorkflowRunning(String workflowId);

    /**
     * Ожидает завершения указанного workflow
     * @param workflowId ID workflow для ожидания
     * @param timeoutMinutes максимальное время ожидания в минутах
     */
    @ActivityMethod
    void waitForWorkflowCompletion(String workflowId, int timeoutMinutes);
}