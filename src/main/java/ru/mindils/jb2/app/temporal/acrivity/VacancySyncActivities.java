package ru.mindils.jb2.app.temporal.acrivity;

import io.temporal.activity.ActivityInterface;
import org.springframework.stereotype.Component;
import ru.mindils.jb2.app.dto.VacancySearchResponseDto;

@ActivityInterface
public interface VacancySyncActivities {

  /**
   * Поиск вакансий на указанной странице
   * @param page номер страницы (начинается с 0)
   * @return результат поиска с вакансиями и метаданными
   */
  VacancySearchResponseDto searchVacancies(int page);

  /**
   * Сохранение детальной информации о вакансии и работодателе
   * @param vacancyId ID вакансии
   */
  void saveVacancy(String vacancyId);

  void saveVacancyState();
}