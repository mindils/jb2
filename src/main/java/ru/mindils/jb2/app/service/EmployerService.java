package ru.mindils.jb2.app.service;

import io.jmix.core.DataManager;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mindils.jb2.app.dto.EmployerDto;
import ru.mindils.jb2.app.entity.Employer;
import ru.mindils.jb2.app.entity.Vacancy;
import ru.mindils.jb2.app.entity.VacancyFilterParams;
import ru.mindils.jb2.app.mapper.EmployerMapper;
import ru.mindils.jb2.app.rest.vacancy.EmployerApiClient;
import ru.mindils.jb2.app.rest.vacancy.VacancyApiClient;

import java.util.List;
import java.util.Map;

@Service
public class EmployerService {

    private final EmployerApiClient employerApiClient;
    private final EmployerMapper employerMapper;
    @PersistenceContext
    private EntityManager entityManager;

    public EmployerService(EmployerApiClient employerApiClient, EmployerMapper employerMapper) {
        this.employerApiClient = employerApiClient;
        this.employerMapper = employerMapper;
    }


    @Transactional
    public Employer update(String number) {
        EmployerDto employerDto = employerApiClient.getById(number);
        Employer employer = employerMapper.toEntity(employerDto);
        return entityManager.merge(employer);
    }
}
