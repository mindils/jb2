package ru.mindils.jb2.app.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class VacancySearchResponseDto {
  private List<VacancyShortDto> items;
  private Integer found;
  private Integer pages;
  private Integer page;
  @JsonProperty("per_page")
  private Integer perPage;
  private Object clusters;
  private Object arguments;
  private Object fixes;
  private Object suggests;
  @JsonProperty("alternate_url")
  private String alternateUrl;
}