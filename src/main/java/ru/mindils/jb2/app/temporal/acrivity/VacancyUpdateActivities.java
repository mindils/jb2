package ru.mindils.jb2.app.temporal.acrivity;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface VacancyUpdateActivities {

  /** Обновить вакансию по записи очереди */
  void update(Long vacancyQueueId);

  /** Взять следующий ID записи очереди (или null, если пусто) */
  Long getNextVacancyId();
}
