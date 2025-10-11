package ru.mindils.jb2.app.service;

import io.jmix.core.DataManager;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mindils.jb2.app.dto.EmployerDto;
import ru.mindils.jb2.app.dto.VacancyDto;
import ru.mindils.jb2.app.dto.VacancySearchResponseDto;
import ru.mindils.jb2.app.entity.Employer;
import ru.mindils.jb2.app.entity.Vacancy;
import ru.mindils.jb2.app.entity.VacancyFilterParams;
import ru.mindils.jb2.app.integration.http.ExternalServiceException;
import ru.mindils.jb2.app.mapper.EmployerMapper;
import ru.mindils.jb2.app.mapper.VacancyMapper;
import ru.mindils.jb2.app.rest.vacancy.EmployerApiClient;
import ru.mindils.jb2.app.rest.vacancy.VacancyApiClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@Service
public class VacancySyncService {
  private static final Logger log = LoggerFactory.getLogger(VacancySyncService.class);

  @PersistenceContext
  private EntityManager entityManager;

  private final VacancyApiClient vacancyApiClient;
  private final DataManager dataManager;
  private final VacancyMapper vacancyMapper;
  private final EmployerApiClient employerApiClient;
  private final EmployerMapper employerMapper;
  private final String DEFAULT_FILTER = "DEFAULT";

  public VacancySyncService(VacancyApiClient vacancyApiClient, DataManager dataManager,
                            VacancyMapper vacancyMapper, EmployerApiClient employerApiClient,
                            EmployerMapper employerMapper) {
    this.vacancyApiClient = vacancyApiClient;
    this.dataManager = dataManager;
    this.vacancyMapper = vacancyMapper;
    this.employerApiClient = employerApiClient;
    this.employerMapper = employerMapper;
  }

  /**
   * Поиск вакансий на указанной странице с применением фильтров из БД (оригинальный метод)
   * @param page номер страницы (начинается с 0)
   * @return результат поиска с вакансиями и метаданными
   */
  public VacancySearchResponseDto searchVacancies(int page) {
    return searchVacancies(page, null);
  }

  /**
   * Поиск вакансий на указанной странице с пользовательскими параметрами
   * @param page номер страницы (начинается с 0)
   * @param customRequestParams дополнительные пользовательские параметры (могут быть null)
   * @return результат поиска с вакансиями и метаданными
   */
  public VacancySearchResponseDto searchVacancies(int page, List<Map<String, String>> customRequestParams) {
    log.info("Searching vacancies on page: {} with custom params: {}", page, customRequestParams);

    // Всегда загружаем базовые параметры из БД
    List<VacancyFilterParams> filterParams = getFilterParams();

    // Строим финальный список параметров, объединяя все источники
    Stream<Map<String, String>> baseParamsStream = filterParams.stream()
        .map(param -> Map.of(param.getParamName(), param.getParamValue()));

    Stream<Map<String, String>> customParamsStream = customRequestParams != null && !customRequestParams.isEmpty()
        ? customRequestParams.stream()
        : Stream.empty();

    Stream<Map<String, String>> pageParamStream = Stream.of(Map.of("page", String.valueOf(page)));

    List<Map<String, String>> requestParams = Stream.concat(
        Stream.concat(baseParamsStream, customParamsStream),
        pageParamStream
    ).toList();

    log.info("Using combined parameters - base filters: {}, custom params: {}, page: {}",
        filterParams.size(),
        customRequestParams != null ? customRequestParams.size() : 0,
        page);

    try {
      VacancySearchResponseDto response = vacancyApiClient.getAll(requestParams);
      log.info("Found {} vacancies on page {}, total found: {}, total pages: {}",
          response.getItems().size(), page, response.getFound(), response.getPages());
      return response;
    } catch (Exception e) {
      log.error("Error searching vacancies on page {}: {}", page, e.getMessage(), e);
      throw new RuntimeException("Failed to search vacancies on page " + page, e);
    }
  }

  /**
   * Сохранение детальной информации о вакансии с работодателем
   * Обрабатывает случай когда вакансия удалена (404) - помечает её как архивную
   *
   * @param vacancyId ID вакансии
   */
  @Transactional
  public void saveVacancyWithDetails(String vacancyId) {
    log.info("Saving vacancy with details: {}", vacancyId);

    try {
      // Получаем детальную информацию о вакансии
      VacancyDto vacancyDto = vacancyApiClient.getById(vacancyId);
      log.debug("Retrieved vacancy details for: {}", vacancyId);

      // Получаем информацию о работодателе
      String employerId = vacancyDto.getEmployer().getId();
      EmployerDto employerDto = employerApiClient.getById(employerId);
      log.debug("Retrieved employer details for: {}", employerId);

      // Преобразуем DTO в сущности
      Vacancy vacancy = vacancyMapper.toEntity(vacancyDto);
      Employer employer = employerMapper.toEntity(employerDto);

      // Сохраняем работодателя (merge для обновления если уже существует)
      Employer mergedEmployer = entityManager.merge(employer);
      log.debug("Saved/updated employer: {}", employerId);

      // Устанавливаем связь и сохраняем вакансию
      vacancy.setEmployer(mergedEmployer);
      entityManager.merge(vacancy);
      log.info("Successfully saved vacancy: {} with employer: {}", vacancyId, employerId);

    } catch (ExternalServiceException e) {
      // Проверяем, это ли 404 ошибка
      if (e.getMessage() != null && e.getMessage().contains("404")) {
        log.warn("Vacancy {} not found on hh.ru (404), marking as archived", vacancyId);
        markVacancyAsArchived(vacancyId);
      } else {
        log.error("External service error while saving vacancy {}: {}", vacancyId, e.getMessage());
        throw new RuntimeException("Failed to save vacancy " + vacancyId + ": " + e.getMessage(), e);
      }
    } catch (Exception e) {
      log.error("Error saving vacancy {}: {}", vacancyId, e.getMessage(), e);
      throw new RuntimeException("Failed to save vacancy " + vacancyId, e);
    }
  }

  /**
   * Помечает вакансию как архивную если она существует в БД
   *
   * @param vacancyId ID вакансии для архивации
   */
  @Transactional
  public void markVacancyAsArchived(String vacancyId) {
    try {
      Optional<Vacancy> vacancyOpt = dataManager.load(Vacancy.class)
          .id(vacancyId)
          .optional();

      if (vacancyOpt.isPresent()) {
        Vacancy vacancy = vacancyOpt.get();
        vacancy.setArchived(true);
        dataManager.save(vacancy);
        log.info("Marked vacancy {} as archived", vacancyId);
      } else {
        log.warn("Vacancy {} not found in database, cannot mark as archived", vacancyId);
      }
    } catch (Exception e) {
      log.error("Error marking vacancy {} as archived: {}", vacancyId, e.getMessage(), e);
      // Не пробрасываем ошибку дальше - это не критично
    }
  }

  /**
   * Получение параметров фильтра из базы данных
   * @return список параметров фильтра
   */
  private List<VacancyFilterParams> getFilterParams() {
    try {
      List<VacancyFilterParams> filterParams = dataManager.load(VacancyFilterParams.class)
          .query("select e from jb2_VacancyFilterParams e where e.vacancyFilter.code = :filterCode")
          .parameter("filterCode", DEFAULT_FILTER)
          .list();
      log.debug("Loaded {} filter parameters", filterParams.size());
      return filterParams;
    } catch (Exception e) {
      log.error("Error loading filter parameters: {}", e.getMessage(), e);
      throw new RuntimeException("Failed to load filter parameters", e);
    }
  }

  // Оставляем старый метод для совместимости, но он больше не используется в workflow
  @Deprecated
  @Transactional
  public Vacancy updateById(String number) {
    log.warn("Using deprecated updateById method. Use saveVacancyWithDetails instead.");
    VacancyDto vacancyDto = vacancyApiClient.getById(number);
    EmployerDto employerDto = employerApiClient.getById(vacancyDto.getEmployer().getId());

    Vacancy vacancy = vacancyMapper.toEntity(vacancyDto);
    Employer employer = employerMapper.toEntity(employerDto);

    Employer mergedEmployer = null;
    if (employer != null) {
      mergedEmployer = entityManager.merge(employer);
      vacancy.setEmployer(mergedEmployer);
    }

    return entityManager.merge(vacancy);
  }
}