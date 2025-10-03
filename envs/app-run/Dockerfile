# Базовый образ для сборки (ARM совместимый)
FROM eclipse-temurin:21-jdk-jammy AS builder

# Установка необходимых инструментов
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /workspace

# ВАЖНО: Увеличиваем heap memory для Node.js
ENV NODE_OPTIONS="--max-old-space-size=4096"

# Копирование Gradle wrapper и конфигурации
COPY gradlew .
COPY gradle ./gradle
COPY build.gradle ./build.gradle
COPY settings.gradle ./settings.gradle

# Делаем gradlew исполняемым
RUN chmod +x ./gradlew

# Загрузка зависимостей
RUN set -e; \
    attempts=0; \
    max_attempts=3; \
    delay_seconds=10; \
    until [ "$attempts" -ge "$max_attempts" ] || ./gradlew dependencies --no-daemon --stacktrace; do \
        attempts=$((attempts+1)); \
        echo "Dependencies download failed (attempt $attempts/$max_attempts). Retrying in ${delay_seconds}s..."; \
        sleep "$delay_seconds"; \
    done

# Копирование исходного кода
COPY src ./src

# Сборка с увеличенной памятью для Gradle и Node.js
RUN set -e; \
    attempts=0; \
    max_attempts=3; \
    delay_seconds=15; \
    until [ "$attempts" -ge "$max_attempts" ] || \
        ./gradlew clean bootJar \
        -Pvaadin.productionMode=true \
        -Dorg.gradle.jvmargs='-Xmx4g -XX:+UseContainerSupport -XX:MaxMetaspaceSize=512m' \
        --no-daemon \
        --stacktrace; do \
        attempts=$((attempts+1)); \
        echo "Build failed (attempt $attempts/$max_attempts). Retrying in ${delay_seconds}s..."; \
        sleep "$delay_seconds"; \
    done; \
    if [ "$attempts" -ge "$max_attempts" ]; then \
        echo "Build failed after $max_attempts attempts"; \
        exit 1; \
    fi

# Runtime образ (меньше размер)
FROM eclipse-temurin:21-jre-jammy

RUN groupadd -r appuser && useradd -r -g appuser appuser

WORKDIR /app

COPY --from=builder /workspace/build/libs/*.jar app.jar

RUN chown -R appuser:appuser /app

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+UseG1GC", \
    "-XX:+OptimizeStringConcat", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", \
    "/app/app.jar"]
