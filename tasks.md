## Задачи для реализации
сделать настройки нейросетей в базе и последовательность чтобы если ошибка в одной llm , подключалась вторая 
развернуть на pi.local
[ ] Привести в порядок страницу вакансий (фильтры кнопки и так далее)
[ ] Сделать фильтры по вакансиям например java = true и так далее

[ ] отобразить информацию о анализе через llm 
[ ] сделать кнопки или выбор like dislike(возможно другие) и для компаний не отображать больше вакинсии этой компании
[ ] обновление организации по кнопке
[ ] подумать куда убрать кнопки обновлений и так далее 
[ ] сделать возможность показывать процесс обновления вакансий и оргоризаций и llm работы

Попробовать сделать отображине статуса сколько осталось загрузить чтобы было понятно сколько ждать 
## На будущее 



package ru.mindils.jb2.app.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.NoopApiKey;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.client.reactive.JdkClientHttpConnector;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class LLMConfiguration {

@Bean
public OpenAiApi openAiApi() {
return OpenAiApi.builder()
.baseUrl("http://localhost:1234")
.apiKey(new NoopApiKey())
.webClientBuilder(WebClient.builder()
.clientConnector(new JdkClientHttpConnector(HttpClient.newBuilder()
.version(HttpClient.Version.HTTP_1_1)
.connectTimeout(Duration.ofSeconds(30))
.build())))
.restClientBuilder(RestClient.builder()
.requestFactory(new JdkClientHttpRequestFactory(HttpClient.newBuilder()
.version(HttpClient.Version.HTTP_1_1)
.connectTimeout(Duration.ofSeconds(30))
.build())))
.build();
}

@Bean
public OpenAiChatModel openAiChatModel(OpenAiApi api) {
return new OpenAiChatModel(
api,
OpenAiChatOptions.builder()
.model("qwen3-30b-a3b-instruct-2507-mlx")        // точно существующая модель
.temperature(0.1)
.maxTokens(64)
.build()
);
}

@Bean
public ChatClient chatClient(OpenAiChatModel model) {
return ChatClient.builder(model).build();
}
}




package ru.mindils.jb2.app.service.analysis.chain.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Component;
import ru.mindils.jb2.app.entity.Vacancy;
import ru.mindils.jb2.app.entity.VacancyAnalysis;
import ru.mindils.jb2.app.service.analysis.AnalysisResultManager;
import ru.mindils.jb2.app.service.analysis.chain.ChainAnalysisStep;
import ru.mindils.jb2.app.service.analysis.chain.ChainStepResult;

@Component
public class PrimaryChainStep implements ChainAnalysisStep {

private static final Logger log = LoggerFactory.getLogger(PrimaryChainStep.class);
private static final String LLM_MODEL = "qwen3-30b-a3b-instruct-2507-mlx";

private final ChatClient chatClient;
private final ObjectMapper objectMapper;
private final AnalysisResultManager analysisResultManager;

public PrimaryChainStep(ChatClient chatClient, ObjectMapper objectMapper, AnalysisResultManager analysisResultManager) {
this.chatClient = chatClient;
this.objectMapper = objectMapper;
this.analysisResultManager = analysisResultManager;
}

@Override
public String getStepId() {
return "primary";
}

@Override
public String getDescription() {
return "Первичный анализ: определение Java, Jmix, AI";
}

@Override
public ChainStepResult execute(Vacancy vacancy, VacancyAnalysis currentAnalysis) {
log.info("Executing primary analysis for vacancy: {}", vacancy.getId());

    try {
      String prompt = buildPrompt(vacancy);

      String llmResponse = chatClient.prompt()
          .user(prompt)
          .options(OpenAiChatOptions.builder().model(LLM_MODEL).build())
          .call()
          .content();

      JsonNode analysisResult = objectMapper.readTree(llmResponse);

      // ОБНОВЛЕННЫЙ КОД - сохраняем в новую структуру
      analysisResultManager.updateStepResult(currentAnalysis, "primary", analysisResult);

      // Проверяем условие остановки
      if (analysisResultManager.shouldStopPipeline(currentAnalysis, "primary")) {
        return ChainStepResult.stop(
            "Вакансия не является Java-позицией",
            analysisResult,
            llmResponse
        );
      }

      return ChainStepResult.success(analysisResult, llmResponse);

    } catch (Exception e) {
      log.error("Error in primary analysis for vacancy {}: {}", vacancy.getId(), e.getMessage(), e);
      throw new RuntimeException("Failed primary analysis", e);
    }
}

private String buildPrompt(Vacancy vacancy) {
return """
Analyze the IT job posting (in Russian) and determine category matches. Return ONLY JSON without additional text.

            Job posting:
            Title: {name}
            Description: {description}
            Key skills: {skills}
            
            Analysis criteria:
            
            1. JAVA (java: true) - position requires DIRECT Java development:
               ✅ INCLUDE only if candidate will write Java code:
                  • Java SE/EE, Spring (Boot/Framework/Data/Security/Cloud)
                  • Hibernate, JPA, JDBC
                  • Maven, Gradle
                  • Backend/microservices in Java
                  • REST API/GraphQL in Java
                  • JUnit, Mockito, TestNG
                  • Java-specific tools: Kafka (Java), RabbitMQ (Java), Elasticsearch (Java client)
            
               ❌ EXCLUDE (java: false):
                  • Android development (even if Java is mentioned)
                  • Kotlin for Android
                  • JavaScript/TypeScript/Node.js/CoffeeScript (NOT Java!)
                  • If Java is in company stack but position is for Python/Go/C#/PHP/Ruby developer
                  • If Java mentioned as "будет плюсом" / "желательно" / "nice to have" but main stack is different
                  • DevOps/QA positions without Java development
            
            2. JMIX (jmix: true) - mentions Jmix platform:
               ✅ INCLUDE:
                  • Jmix, Jmix Studio, Jmix framework
                  • CUBA Platform (Jmix predecessor)
                  • Haulmont Jmix
            
               ❌ EXCLUDE:
                  • Similar names not related to Jmix
            
            3. AI (ai: true) - position requires AI/ML work:
               ✅ INCLUDE:
                  • Искусственный интеллект, машинное обучение, нейронные сети, нейросети
                  • AI, ML, Machine Learning, Deep Learning, Neural Networks
                  • LLM, GPT, ChatGPT, Claude, Gemini, LangChain, YandexGPT, GigaChat
                  • TensorFlow, PyTorch, Keras, scikit-learn, JAX
                  • Data Science with ML focus
                  • NLP, Computer Vision, Reinforcement Learning
                  • AI Engineer, ML Engineer, AI Researcher, ML-инженер
                  • AI solutions development/integration
            
               ❌ EXCLUDE:
                  • Simple analytics without ML
                  • BI without ML components
                  • Data Engineering without ML
            
            Response format (strict JSON):
            {
              "java": boolean,
              "jmix": boolean,
              "ai": boolean
            }
            """
        .replace("{name}", vacancy.getName())
        .replace("{description}", truncateText(vacancy.getDescription(), 2000))
        .replace("{skills}", vacancy.getKeySkillsStr());
}

private String truncateText(String text, int maxLength) {
if (text == null || text.length() <= maxLength) return text;
return text.substring(0, maxLength) + "...";
}
}


package ru.mindils.jb2.app.service.analysis.chain.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Component;
import ru.mindils.jb2.app.entity.Vacancy;
import ru.mindils.jb2.app.entity.VacancyAnalysis;
import ru.mindils.jb2.app.service.analysis.AnalysisResultManager;
import ru.mindils.jb2.app.service.analysis.chain.ChainAnalysisStep;
import ru.mindils.jb2.app.service.analysis.chain.ChainStepResult;

@Component
public class SocialChainStep implements ChainAnalysisStep {

private static final Logger log = LoggerFactory.getLogger(SocialChainStep.class);
private static final String LLM_MODEL = "qwen3-30b-a3b-instruct-2507-mlx";

private final ChatClient chatClient;
private final ObjectMapper objectMapper;
private final AnalysisResultManager analysisResultManager;

public SocialChainStep(ChatClient chatClient,
ObjectMapper objectMapper,
AnalysisResultManager analysisResultManager) {
this.chatClient = chatClient;
this.objectMapper = objectMapper;
this.analysisResultManager = analysisResultManager;
}

@Override
public String getStepId() {
return "social";
}

@Override
public String getDescription() {
return "Социальный анализ: формат работы, домены, социальная значимость";
}

@Override
public ChainStepResult execute(Vacancy vacancy, VacancyAnalysis currentAnalysis) {
log.info("Executing social analysis for vacancy: {}", vacancy.getId());

    try {
      String prompt = buildPrompt(vacancy);

      String llmResponse = chatClient.prompt()
          .user(prompt)
          .options(OpenAiChatOptions.builder().model(LLM_MODEL).build())
          .call()
          .content();

      // Парсим ответ LLM (и нормализуем поля с дефолтами)
      JsonNode raw = objectMapper.readTree(llmResponse);
      String workMode = asTextOr(raw, "work_mode", "unknown");
      String domains = asTextOr(raw, "domains", "unknown");
      boolean sociallySignificant = asBooleanOr(raw, "socially_significant", false);

      ObjectNode analysisResult = objectMapper.createObjectNode()
          .put("work_mode", workMode)
          .put("domains", domains)
          .put("socially_significant", sociallySignificant);

      // ОБНОВЛЁННО: сохраняем результат шага в analysis_metadata/step_results
      analysisResultManager.updateStepResult(currentAnalysis, "social", analysisResult);

      // Локальное условие остановки (как было раньше)
      if ("office".equals(workMode) && !sociallySignificant) {
        return ChainStepResult.stop(
            "Только офисная работа в коммерческом проекте - не подходит",
            analysisResult,
            llmResponse
        );
      }

      // Дополнительно: централизованные правила остановки (если настроены)
      if (analysisResultManager.shouldStopPipeline(currentAnalysis, "social")) {
        return ChainStepResult.stop(
            "Сработали правила остановки для шага 'social'",
            analysisResult,
            llmResponse
        );
      }

      return ChainStepResult.success(analysisResult, llmResponse);

    } catch (Exception e) {
      log.error("Error in social analysis for vacancy {}: {}", vacancy.getId(), e.getMessage(), e);
      throw new RuntimeException("Failed social analysis", e);
    }
}

private String buildPrompt(Vacancy vacancy) {
return """
Analyze the IT job posting (in Russian) and extract work format and project domain information. Return ONLY flat JSON without additional text.

            Job posting:
            Title: {name}
            Description: {description}
            Key skills: {skills}
            
            Analysis criteria:
            
            1. WORK MODE (work_mode) - single value:
               • "remote" - полностью удаленная работа, remote work, дистанционно, из дома
               • "office" - только офис, on-site only, без удаленки
               • "hybrid" - гибрид, hybrid, совмещение офиса и удаленки (без указания конкретных дней)
               • "hybrid_2_3" - гибрид 2 дня офис / 3 дня дома
               • "hybrid_3_2" - гибрид 3 дня офис / 2 дня дома
               • "hybrid_4_1" - гибрид 4 дня офис / 1 день дома
               • "hybrid_flexible" - гибрид с гибким графиком посещения офиса
               • "flexible" - полностью гибкий график (сам выбираешь когда офис/дом)
               • "unknown" - формат не указан или неясен
            
            2. PROJECT DOMAINS (domains) - pipe-separated string of applicable domains:
               • "healthcare" - медицина, здравоохранение, DICOM, медицинские системы, телемедицина, фарма
               • "education" - образование, EdTech, обучение, e-learning, университеты
               • "ecology" - экология, окружающая среда, зеленые технологии
               • "energy" - энергетика, электроэнергия, ЖКХ, utilities
               • "government" - госсектор, госуслуги, муниципальные услуги
               • "social" - социальные услуги, пенсии, пособия, социальная защита
               • "transport" - транспорт, логистика для населения, общественный транспорт
               • "safety" - безопасность, МЧС, пожарная безопасность
               • "employment" - трудоустройство, поиск работы, HR для населения
               • "insurance" - страхование (медицинское, социальное, автострахование)
               • "finance" - финансовые услуги для населения, банки, пенсионные фонды
               • "agriculture" - сельское хозяйство, продовольственная безопасность
               • "science" - научные исследования, R&D
               • "humanitarian" - гуманитарная помощь, благотворительность, НКО
               • "commercial" - коммерческий проект без социальной значимости
               • "unknown" - направление не определено
            
            3. SOCIALLY SIGNIFICANT (socially_significant) - boolean:
               • true - если проект имеет социальную значимость (любой домен кроме "commercial" и "unknown")
               • false - если проект чисто коммерческий или неизвестен
            
            Response format (strict flat JSON):
            {
              "work_mode": string,
              "domains": string,
              "socially_significant": boolean
            }
            """
        .replace("{name}", vacancy.getName())
        .replace("{description}", truncateText(vacancy.getDescription(), 2000))
        .replace("{skills}", vacancy.getKeySkillsStr());
}

private String truncateText(String text, int maxLength) {
if (text == null || text.length() <= maxLength) return text;
return text.substring(0, maxLength) + "...";
}

private String asTextOr(JsonNode node, String field, String def) {
JsonNode v = node != null ? node.get(field) : null;
return v != null && !v.isNull() ? v.asText(def) : def;
}

private boolean asBooleanOr(JsonNode node, String field, boolean def) {
JsonNode v = node != null ? node.get(field) : null;
return v != null && !v.isNull() ? v.asBoolean(def) : def;
}
}



application.properties

# Локальный OpenAI-совместимый сервер (Qwen)
spring.ai.openai.base-url=http://pi.local:1234/
spring.ai.openai.api-key=dummy

# Модель по умолчанию - используем такое же имя как в curl
spring.ai.openai.chat.options.model=qwen3-30b-a3b-instruct-2507-mlx
spring.ai.openai.chat.options.temperature=0.0
spring.ai.openai.chat.options.max-tokens=1000




у меня есть такие классы и настройки в application.properties

мне бы хотелось сделать доработку


я хочу
1. модель по умолчанию
2. модель из базы занных чтобы можно было установить 
