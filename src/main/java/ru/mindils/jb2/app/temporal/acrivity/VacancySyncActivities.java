package ru.mindils.jb2.app.temporal.acrivity;

import io.temporal.activity.ActivityInterface;
import ru.mindils.jb2.app.dto.VacancySearchResponseDto;

import java.util.List;
import java.util.Map;

@ActivityInterface
public interface VacancySyncActivities {
  /**
   * Поиск вакансий на указанной странице с пользовательскими параметрами
   * @param page номер страницы (начинается с 0)
   * @param requestParams дополнительные параметры запроса (может быть null)
   * @return результат поиска с вакансиями и метаданными
   */
  VacancySearchResponseDto searchVacancies(int page, List<Map<String, String>> requestParams);

  /**
   * Сохранение детальной информации о вакансии и работодателе
   * @param vacancyId ID вакансии
   */
  void saveVacancy(String vacancyId);

  void saveVacancyState();
}
