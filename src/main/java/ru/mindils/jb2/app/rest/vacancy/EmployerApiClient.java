package ru.mindils.jb2.app.rest.vacancy;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.mindils.jb2.app.dto.EmployerDto;
import ru.mindils.jb2.app.dto.VacancyDto;
import ru.mindils.jb2.app.entity.Employer;
import ru.mindils.jb2.app.entity.Vacancy;
import ru.mindils.jb2.app.integration.http.JsonHttpClient;
import ru.mindils.jb2.app.mapper.EmployerMapper;
import ru.mindils.jb2.app.mapper.VacancyMapper;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.joining;

@Service
public class EmployerApiClient {
    private static final String EMPLOYER_API_URL = "https://api.hh.ru/employers";

    @Autowired
    JsonHttpClient client;

    @Autowired
    private VacancyMapper vacancyMapper;
    @Autowired
    private EmployerMapper employerMapper;

    @SneakyThrows
    public EmployerDto getById(String id) {
        EmployerDto dto = client.retrieve(URI.create(EMPLOYER_API_URL + "/" + id), EmployerDto.class).data();
        return dto;
    }
}
