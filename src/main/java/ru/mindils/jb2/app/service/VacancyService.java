package ru.mindils.jb2.app.service;

import io.jmix.core.DataManager;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mindils.jb2.app.dto.EmployerDto;
import ru.mindils.jb2.app.dto.VacancyDto;
import ru.mindils.jb2.app.entity.Employer;
import ru.mindils.jb2.app.entity.Vacancy;
import ru.mindils.jb2.app.entity.VacancyFilterParams;
import ru.mindils.jb2.app.mapper.EmployerMapper;
import ru.mindils.jb2.app.mapper.VacancyMapper;
import ru.mindils.jb2.app.rest.vacancy.EmployerApiClient;
import ru.mindils.jb2.app.rest.vacancy.VacancyApiClient;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class VacancyService {

    private final VacancyApiClient vacancyApiClient;
    private final DataManager dataManager;
    private final VacancyMapper vacancyMapper;
    private final EmployerApiClient employerApiClient;
    private final EmployerMapper employerMapper;
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

    @Transactional
    public List<Vacancy> updateAll() {
        List<VacancyFilterParams> filter = dataManager.load(VacancyFilterParams.class)
                .query("select e from jb2_VacancyFilterParams e where e.vacancyFilter.id = :filterId")
                .parameter("filterId", 1)
                .list();

        List<Map<String, String>> filterMap = filter
                .stream()
                .map(param -> Map.of(param.getParamName(), param.getParamValue()))
                .toList();

        List<Vacancy> vacancy = vacancyApiClient.getAll(filterMap);

        return List.of();
    }
}
