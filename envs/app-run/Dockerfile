# Dockerfile оптимизированный для слабого сервера (1-2 CPU, 2GB RAM)
# Сборка займет 20-40 минут, но будет стабильной

# ============================================
# Builder Stage - сборка приложения
# ============================================
FROM eclipse-temurin:21-jdk-jammy AS builder

# Установка curl для healthcheck
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /workspace

# Настройки для слабого сервера
# Увеличиваем память для Node.js для сборки Vaadin frontend
# После сборки это не используется в runtime
ENV NODE_OPTIONS="--max-old-space-size=2048"

# ============================================
# Слой 1: Gradle Wrapper (кешируется)
# ============================================
COPY gradlew .
COPY gradle ./gradle
RUN chmod +x ./gradlew

# ============================================
# Слой 2: Gradle конфигурация (кешируется)
# ============================================
COPY build.gradle settings.gradle ./

# ============================================
# Слой 3: Загрузка зависимостей
# Используем retry логику т.к. на слабом сервере могут быть таймауты
# ============================================
RUN set -e; \
    attempts=0; \
    max_attempts=3; \
    until [ "$attempts" -ge "$max_attempts" ] || \
        ./gradlew dependencies \
        --no-daemon \
        --no-parallel \
        --max-workers=1 \
        --stacktrace; do \
        attempts=$((attempts+1)); \
        echo "Попытка $attempts/$max_attempts не удалась, повтор через 15 сек..."; \
        sleep 15; \
    done

# ============================================
# Слой 4: Копирование исходного кода
# ============================================
COPY src ./src

# ============================================
# Слой 5: Сборка приложения
# Критически важные настройки для слабого сервера:
# - Xmx1536m: максимум 1.5GB для Gradle (оставляем 400MB системе)
# - no-parallel: без параллелизации
# - max-workers=1: только 1 воркер
# - no-daemon: без daemon процесса (экономит память)
# ============================================
RUN set -e; \
    attempts=0; \
    max_attempts=3; \
    until [ "$attempts" -ge "$max_attempts" ] || \
        ./gradlew bootJar \
        -Pvaadin.productionMode=true \
        -Dorg.gradle.jvmargs='-Xmx1536m -Xms512m -XX:+UseSerialGC -XX:MaxMetaspaceSize=256m' \
        --no-daemon \
        --no-parallel \
        --max-workers=1 \
        --stacktrace; do \
        attempts=$((attempts+1)); \
        echo "Сборка не удалась (попытка $attempts/$max_attempts)"; \
        echo "Повтор через 20 секунд..."; \
        rm -rf build/tmp 2>/dev/null || true; \
        sleep 20; \
    done; \
    if [ "$attempts" -ge "$max_attempts" ]; then \
        echo "Сборка провалилась после $max_attempts попыток"; \
        exit 1; \
    fi

# Очистка ненужных файлов для экономии места
RUN rm -rf /root/.gradle/caches/*/generated-gradle-jars \
    && rm -rf /root/.gradle/caches/*/scripts \
    && rm -rf /workspace/.gradle \
    && find /root/.gradle/caches/ -name "*.lock" -delete

# ============================================
# Runtime Stage - финальный образ
# ============================================
FROM eclipse-temurin:21-jre-jammy

# Создание непривилегированного пользователя
RUN groupadd -r appuser && useradd -r -g appuser appuser

WORKDIR /app

# Копирование только JAR файла
COPY --from=builder /workspace/build/libs/*.jar app.jar

# Установка прав
RUN chown -R appuser:appuser /app

USER appuser

EXPOSE 8080

# Упрощенный healthcheck (меньше нагрузки)
HEALTHCHECK --interval=45s --timeout=5s --start-period=120s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM настройки для слабого сервера
# MaxRAMPercentage=60 - используем максимум 60% от доступной памяти
# SerialGC - простой GC, меньше overhead для малого кол-ва ядер
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=60.0", \
    "-XX:InitialRAMPercentage=30.0", \
    "-XX:+UseSerialGC", \
    "-XX:MaxMetaspaceSize=256m", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.backgroundpreinitializer.ignore=true", \
    "-jar", \
    "/app/app.jar"]