# Базовый образ для сборки (ARM совместимый)
FROM eclipse-temurin:21-jdk-jammy AS builder

# Установка необходимых инструментов
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /workspace

# Копирование Gradle wrapper и конфигурации
COPY gradlew .
COPY gradle ./gradle
COPY build.gradle ./build.gradle
COPY settings.gradle ./settings.gradle

# Делаем gradlew исполняемым
RUN chmod +x ./gradlew

# Загрузка зависимостей с retry логикой (из-за возможных проблем с сетью)
RUN set -e; \
    attempts=0; \
    max_attempts=3; \
    delay_seconds=10; \
    until [ "$attempts" -ge "$max_attempts" ] || ./gradlew dependencies --no-daemon --stacktrace; do \
        attempts=$((attempts+1)); \
        echo "Dependencies download failed (attempt $attempts/$max_attempts). Retrying in ${delay_seconds}s..."; \
        sleep "$delay_seconds"; \
    done; \
    if [ "$attempts" -ge "$max_attempts" ]; then \
        echo "Failed to download dependencies after $max_attempts attempts"; \
        exit 1; \
    fi

# Копирование исходного кода
COPY src ./src

# Сборка приложения с Vaadin production mode
# -Xmx2g важен для Vaadin frontend сборки
# Для Raspberry Pi с ограниченной памятью можно снизить до -Xmx1g, но сборка может быть медленнее
RUN set -e; \
    attempts=0; \
    max_attempts=3; \
    delay_seconds=15; \
    until [ "$attempts" -ge "$max_attempts" ] || \
        ./gradlew clean bootJar \
        -Pvaadin.productionMode=true \
        -Dorg.gradle.jvmargs='-Xmx2g -XX:+UseContainerSupport' \
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

# Создание пользователя для безопасности
RUN groupadd -r appuser && useradd -r -g appuser appuser

WORKDIR /app

# Копирование собранного jar
COPY --from=builder /workspace/build/libs/*.jar app.jar

# Смена владельца файлов
RUN chown -R appuser:appuser /app

# Переключение на непривилегированного пользователя
USER appuser

EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM оптимизация для контейнера
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+UseG1GC", \
    "-XX:+OptimizeStringConcat", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", \
    "/app/app.jar"]