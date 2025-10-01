#!/bin/bash

SERVER="mindils@104.253.1.57"
LOCAL_PORT=4000
REMOTE_PORT=4000
PID_FILE="/tmp/ssh_tunnel_4000.pid"

start_tunnel() {
    # Проверяем, не запущен ли уже туннель
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if ps -p "$PID" > /dev/null 2>&1; then
            echo "❌ Туннель уже запущен (PID: $PID)"
            return 1
        else
            # Удаляем старый PID файл
            rm -f "$PID_FILE"
        fi
    fi

    # Запускаем SSH туннель
    echo "🚀 Запускаем SSH туннель..."
    ssh -f -N -L ${LOCAL_PORT}:localhost:${REMOTE_PORT} ${SERVER}

    # Даём время на запуск
    sleep 1

    # Сохраняем PID
    PID=$(pgrep -f "ssh -f -N -L ${LOCAL_PORT}:localhost:${REMOTE_PORT} ${SERVER}")
    if [ -n "$PID" ]; then
        echo "$PID" > "$PID_FILE"
        echo "✅ Туннель запущен успешно (PID: $PID)"
        echo "📍 Доступ: http://localhost:${LOCAL_PORT}"
    else
        echo "❌ Ошибка запуска туннеля"
        return 1
    fi
}

stop_tunnel() {
    if [ ! -f "$PID_FILE" ]; then
        echo "❌ PID файл не найден. Туннель не запущен."
        return 1
    fi

    PID=$(cat "$PID_FILE")

    if ps -p "$PID" > /dev/null 2>&1; then
        echo "🛑 Останавливаем туннель (PID: $PID)..."
        kill "$PID"
        rm -f "$PID_FILE"
        echo "✅ Туннель остановлен"
    else
        echo "⚠️  Процесс с PID $PID не найден"
        rm -f "$PID_FILE"
    fi
}

status_tunnel() {
    if [ ! -f "$PID_FILE" ]; then
        echo "❌ Туннель не запущен"
        return 1
    fi

    PID=$(cat "$PID_FILE")

    if ps -p "$PID" > /dev/null 2>&1; then
        echo "✅ Туннель работает (PID: $PID)"
        echo "📍 Доступ: http://localhost:${LOCAL_PORT}"
        
        # Проверяем доступность
        if command -v curl > /dev/null 2>&1; then
            if curl -s -o /dev/null -w "%{http_code}" http://localhost:${LOCAL_PORT} > /dev/null 2>&1; then
                echo "🌐 Сервис доступен"
            else
                echo "⚠️  Сервис не отвечает"
            fi
        fi
    else
        echo "❌ Процесс не найден, но PID файл существует"
        rm -f "$PID_FILE"
        return 1
    fi
}

restart_tunnel() {
    echo "🔄 Перезапускаем туннель..."
    stop_tunnel
    sleep 1
    start_tunnel
}

case "$1" in
    start)
        start_tunnel
        ;;
    stop)
        stop_tunnel
        ;;
    status)
        status_tunnel
        ;;
    restart)
        restart_tunnel
        ;;
    *)
        echo "Использование: $0 {start|stop|status|restart}"
        echo ""
        echo "Команды:"
        echo "  start   - Запустить SSH туннель"
        echo "  stop    - Остановить SSH туннель"
        echo "  status  - Проверить статус туннеля"
        echo "  restart - Перезапустить туннель"
        exit 1
        ;;
esac
