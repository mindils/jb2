#!/bin/bash
set -e

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$PROJECT_DIR"

echo "=== JB2 Deploy ==="
echo "Время: $(date '+%Y-%m-%d %H:%M:%S')"

# 1. Git pull
echo "[1/5] Git pull..."
git pull

# 2. Остановка старого контейнера
echo "[2/5] Остановка контейнера..."
cd envs/app-run
docker compose down 2>/dev/null || true

# 3. Очистка (БЕЗ удаления dangling образов - они нужны для кеша!)
echo "[3/5] Очистка остановленных контейнеров..."
docker container prune -f

# Опционально: удаляем старые образы jb2, которые не используются и созданы > 7 дней назад
# Это освобождает место, но сохраняет недавние образы для отката
echo "Очистка старых неиспользуемых образов jb2 (>7 дней)..."
docker images jb2 --format "{{.ID}} {{.CreatedAt}}" | while read id created_date; do
    # Парсим дату создания (формат может отличаться)
    created_timestamp=$(date -d "$created_date" +%s 2>/dev/null || echo "0")
    current_timestamp=$(date +%s)
    days_old=$(( (current_timestamp - created_timestamp) / 86400 ))

    if [ "$days_old" -gt 7 ]; then
        echo "  Удаляю образ $id (возраст: $days_old дней)"
        docker rmi -f "$id" 2>/dev/null || true
    fi
done

# 4. Сборка
echo "[4/5] Сборка образа (20-40 мин)..."
export DOCKER_BUILDKIT=0
docker compose build 2>&1 | tee /tmp/jb2-build.log

# 5. Запуск
echo "[5/5] Запуск контейнера..."
docker compose up -d

# Ожидание
sleep 30

echo "=== Deploy завершен ==="
echo "Приложение: http://localhost:8085"
echo "Логи: docker logs -f jb2"
echo ""
echo "💡 Совет: для очистки старых логов запустите: ./cleanup-logs.sh"