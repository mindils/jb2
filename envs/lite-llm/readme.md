# Тест к группе моделей
curl -X POST http://localhost:4000/chat/completions \
-H "Authorization: Bearer $LITELLM_MASTER_KEY" \
-H "Content-Type: application/json" \
-d '{
"model": "chat-models",
"messages": [{"role": "user", "content": "Привет!"}]
}'

# Сохраните ключ в переменную для удобства
export LITELLM_KEY="sk-1234-your-secure-key-here"

# Проверка health
curl http://localhost:4000/health

# Список моделей
curl -H "Authorization: Bearer $LITELLM_KEY" \
http://localhost:4000/models

# Запрос к конкретной модели
curl -X POST http://localhost:4000/chat/completions \
-H "Authorization: Bearer $LITELLM_KEY" \
-H "Content-Type: application/json" \
-d '{
"model": "gpt-oss-120b",
"messages": [{"role": "user", "content": "Привет!"}]
}'

# Запрос с авто-выбором
curl -X POST http://localhost:4000/chat/completions \
-H "Authorization: Bearer $LITELLM_KEY" \
-H "Content-Type: application/json" \
-d '{
"model": "chat-models",
"messages": [{"role": "user", "content": "Привет!"}]
}'
