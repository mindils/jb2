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

# 3. Очистка старых контейнеров и образов
echo "[3/5] Очистка старых ресурсов..."
# Удаляем остановленные контейнеры
docker container prune -f
# Удаляем dangling образы
docker image prune -f
# Удаляем старые образы jb2 (кроме latest)
docker images jb2 --format "{{.ID}} {{.Tag}}" | grep -v latest | awk '{print $1}' | xargs -r docker rmi -f 2>/dev/null || true

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