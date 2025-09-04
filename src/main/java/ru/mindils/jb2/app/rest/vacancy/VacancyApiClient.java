package ru.mindils.jb2.app.rest.vacancy;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.mindils.jb2.app.dto.VacancyDto;
import ru.mindils.jb2.app.entity.Vacancy;
import ru.mindils.jb2.app.integration.http.JsonHttpClient;
import ru.mindils.jb2.app.mapper.VacancyMapper;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.joining;

@Service
public class VacancyApiClient {
    private static final String VACANCY_API_URL = "https://api.hh.ru/vacancies";

    @Autowired
    JsonHttpClient client;

    @Autowired
    private VacancyMapper vacancyMapper;

    @SneakyThrows
    public VacancyDto getById(String id) {
        VacancyDto dto = client.retrieve(URI.create(VACANCY_API_URL + "/" + id), VacancyDto.class).data();
        return dto;
    }

    @SneakyThrows
    public List<Vacancy> getAll(List<Map<String, String>> params) {
      client.retrieve(buildURIWithParams(VACANCY_API_URL, params), Object.class).data();
        return List.of();
    }

    private URI buildURIWithParams(String uri, List<Map<String, String>> params) {
        return URI.create(uri
                + params.stream()
                .flatMap(map -> map.entrySet().stream())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(joining("&", "?", "")));
    }
}
