# Вручную (20-40 мин)
./envs/app-run/deploy.sh
# Только для jb2 контейнера!
cd envs/app-run

# Запуск
docker compose up -d

# Остановка
docker compose down

# Перезапуск
docker restart jb2

# Логи
docker logs -f jb2

# Пересборка
docker compose build --no-cache
docker compose up -d

# Статистика
docker stats jb2

# Сделать исполняемым
chmod +x envs/app-run/web-control.sh

# Запустить
./envs/app-run/web-control.sh start

# Остановить
./envs/app-run/web-control.sh stop

# Статус
./envs/app-run/web-control.sh status

# Перезапуск
./envs/app-run/web-control.sh restart

# Логи
./envs/app-run/web-control.sh logs