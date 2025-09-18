package ru.mindils.jb2.app.service;

import io.jmix.core.DataManager;
import ru.mindils.jb2.app.integration.http.ExternalServiceException;

import java.time.LocalDateTime;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mindils.jb2.app.dto.EmployerDto;
import ru.mindils.jb2.app.dto.VacancyDto;
import ru.mindils.jb2.app.dto.VacancySearchResponseDto;
import ru.mindils.jb2.app.entity.Employer;
import ru.mindils.jb2.app.entity.Vacancy;
import ru.mindils.jb2.app.entity.VacancyFilterParams;
import ru.mindils.jb2.app.mapper.EmployerMapper;
import ru.mindils.jb2.app.mapper.VacancyMapper;
import ru.mindils.jb2.app.rest.vacancy.EmployerApiClient;
import ru.mindils.jb2.app.rest.vacancy.VacancyApiClient;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Service
public class VacancyService {

  private final VacancyApiClient vacancyApiClient;
  private final DataManager dataManager;
  private final VacancyMapper vacancyMapper;
  private final EmployerApiClient employerApiClient;
  private final EmployerMapper employerMapper;
  private final String DEFAULT_FILTER = "DEFAULT";


  @PersistenceContext
  private EntityManager entityManager;

  public VacancyService(VacancyApiClient vacancyApiClient, DataManager dataManager, VacancyMapper vacancyMapper, EmployerApiClient employerApiClient, EmployerMapper employerMapper) {
    this.vacancyApiClient = vacancyApiClient;
    this.dataManager = dataManager;
    this.vacancyMapper = vacancyMapper;
    this.employerApiClient = employerApiClient;
    this.employerMapper = employerMapper;
  }

  @Transactional
  public Vacancy update(String number) {
    try {
      // 1) тянем вакансию
      VacancyDto vacancyDto = vacancyApiClient.getById(number);

      // 2) тянем работодателя (может тоже вернуть 404 — обработаем ниже отдельно)
      Employer mergedEmployer = null;
      try {
        EmployerDto employerDto = employerApiClient.getById(vacancyDto.getEmployer().getId());
        Employer employer = employerMapper.toEntity(employerDto);
        mergedEmployer = entityManager.merge(employer);
      } catch (ExternalServiceException ex) {
        // если у работодателя 404 — просто продолжаем без него
        if (ex.statusCode() != 404 && !(ex.getMessage() != null && ex.getMessage().contains(" 404"))) {
          throw ex; // это не 404 → пробрасываем
        }
      }

      // 3) маппим и сохраняем вакансию
      Vacancy vacancy = vacancyMapper.toEntity(vacancyDto);
      if (mergedEmployer != null) {
        vacancy.setEmployer(mergedEmployer);
      }

      return entityManager.merge(vacancy);

    } catch (ExternalServiceException e) {
      // если API по вакансии вернул 404 — архивируем локальную запись и выходим без ошибки
      boolean is404 = e.statusCode() == 404 || (e.getMessage() != null && e.getMessage().contains(" 404"));
      if (is404) {
        return markVacancyArchived(number);
      }
      throw e;
    }
  }

  private Vacancy markVacancyArchived(String vacancyId) {
    Vacancy v = entityManager.find(Vacancy.class, vacancyId);
    if (v == null) {
      v = new Vacancy();
      v.setId(vacancyId);
    }
    v.setArchived(true);
    return entityManager.merge(v);
  }

  @Transactional
  public List<Vacancy> updateAll() {
    List<VacancyFilterParams> filter = dataManager.load(VacancyFilterParams.class)
        .query("select e from jb2_VacancyFilterParams e where e.vacancyFilter.code = :filterCode")
        .parameter("filterCode", DEFAULT_FILTER)
        .list();

//        int page = 0;
//        do {
//
//          List<Map<String, String>> filterMap = Stream.concat(
//              filter.stream().map(param -> Map.of(param.getParamName(), param.getParamValue())),
//              Stream.of(Map.of("page", page))
//          ).toList();
//
//          // получаем вакансии по фильтру
//          VacancySearchResponseDto response = vacancyApiClient.getAll(filterMap);
//
//          // получить в цикле детальную вакансию
//          // получить детальную организицию
//
//          // сохранить в бузе данных
//
//        } while (filter.isEmpty());

    List<Map<String, String>> filterMap = Stream.concat(
        filter.stream().map(param -> Map.of(param.getParamName(), param.getParamValue())),
        Stream.of(Map.of("page", "2"))
    ).toList();

//        filterMap.add(Map.of("page", "2"));

    VacancySearchResponseDto response = vacancyApiClient.getAll(filterMap);

    return List.of();
  }
}
