package ru.mindils.jb2.app.service.analysis.chain.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import ru.mindils.jb2.app.entity.Vacancy;
import ru.mindils.jb2.app.entity.VacancyAnalysis;
import ru.mindils.jb2.app.service.analysis.AnalysisResultManager;
import ru.mindils.jb2.app.service.analysis.chain.ChainAnalysisStep;
import ru.mindils.jb2.app.service.analysis.chain.ChainStepResult;

@Component
public class SalaryAnalysisStep implements ChainAnalysisStep {

  private final AnalysisResultManager analysisResultManager;
  private final ObjectMapper objectMapper;

  public SalaryAnalysisStep(AnalysisResultManager analysisResultManager, ObjectMapper objectMapper) {
    this.analysisResultManager = analysisResultManager;
    this.objectMapper = objectMapper;
  }

  @Override
  public String getStepId() {
    return "salary";
  }

  @Override
  public String getDescription() {
    return "Анализ зарплаты: извлечение и нормализация данных о зарплате";
  }

  @Override
  public ChainStepResult execute(Vacancy vacancy, VacancyAnalysis currentAnalysis) {
    try {
      JsonNode salaryData = extractSalaryData(vacancy);
      analysisResultManager.updateStepResult(currentAnalysis, getStepId(), salaryData);

      return ChainStepResult.success(salaryData, "salary_extracted");
    } catch (Exception e) {
      throw new RuntimeException("Failed salary analysis", e);
    }
  }

  private JsonNode extractSalaryData(Vacancy vacancy) {
    // Логика извлечения данных о зарплате
    var salaryNode = objectMapper.createObjectNode();

    if (vacancy.getSalary() != null) {
      JsonNode salary = vacancy.getSalary();
      salaryNode.put("has_salary", true);
      salaryNode.put("salary_from", salary.path("from").asInt(0));
      salaryNode.put("salary_to", salary.path("to").asInt(0));
      salaryNode.put("currency", salary.path("currency").asText(""));
    } else {
      salaryNode.put("has_salary", false);
    }

    return salaryNode;
  }
}