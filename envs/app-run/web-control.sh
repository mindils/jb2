#!/bin/bash
# Простое управление веб-интерфейсом JB2

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PID_FILE="/tmp/jb2-web.pid"
LOG_FILE="/tmp/jb2-web.log"

start() {
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if ps -p "$PID" > /dev/null 2>&1; then
            echo "Веб-интерфейс уже запущен (PID: $PID)"
            echo "URL: http://localhost:5000"
            return 0
        fi
    fi

    echo "Запуск веб-интерфейса..."
    nohup python3 "$SCRIPT_DIR/web-deploy.py" > "$LOG_FILE" 2>&1 &
    echo $! > "$PID_FILE"
    sleep 2

    if ps -p $(cat "$PID_FILE") > /dev/null 2>&1; then
        echo "✓ Веб-интерфейс запущен (PID: $(cat $PID_FILE))"
        echo "✓ URL: http://localhost:5000"
        echo "✓ Логи: $LOG_FILE"
    else
        echo "✗ Ошибка запуска, смотрите логи: $LOG_FILE"
        rm -f "$PID_FILE"
        return 1
    fi
}

stop() {
    if [ ! -f "$PID_FILE" ]; then
        echo "Веб-интерфейс не запущен"
        return 0
    fi

    PID=$(cat "$PID_FILE")
    if ps -p "$PID" > /dev/null 2>&1; then
        echo "Остановка веб-интерфейса (PID: $PID)..."
        kill "$PID"
        sleep 1

        if ps -p "$PID" > /dev/null 2>&1; then
            kill -9 "$PID" 2>/dev/null
        fi

        rm -f "$PID_FILE"
        echo "✓ Веб-интерфейс остановлен"
    else
        echo "Процесс не найден, очистка PID файла"
        rm -f "$PID_FILE"
    fi
}

status() {
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if ps -p "$PID" > /dev/null 2>&1; then
            echo "✓ Веб-интерфейс работает (PID: $PID)"
            echo "  URL: http://localhost:5000"
            echo "  Логи: $LOG_FILE"
            echo ""
            echo "Использование памяти:"
            ps -p "$PID" -o pid,rss,cmd | tail -n 1 | awk '{printf "  PID: %s, RAM: %.1f MB\n", $1, $2/1024}'
            return 0
        else
            echo "✗ PID файл есть, но процесс не запущен"
            rm -f "$PID_FILE"
            return 1
        fi
    else
        echo "✗ Веб-интерфейс не запущен"
        return 1
    fi
}

logs() {
    if [ -f "$LOG_FILE" ]; then
        echo "Последние 20 строк логов:"
        tail -20 "$LOG_FILE"
        echo ""
        echo "Все логи: tail -f $LOG_FILE"
    else
        echo "Лог файл не найден"
    fi
}

restart() {
    stop
    sleep 1
    start
}

case "$1" in
    start)
        start
        ;;
    stop)
        stop
        ;;
    restart)
        restart
        ;;
    status)
        status
        ;;
    logs)
        logs
        ;;
    *)
        echo "Использование: $0 {start|stop|restart|status|logs}"
        echo ""
        echo "Команды:"
        echo "  start   - Запустить веб-интерфейс"
        echo "  stop    - Остановить веб-интерфейс"
        echo "  restart - Перезапустить"
        echo "  status  - Проверить статус"
        echo "  logs    - Показать логи"
        exit 1
        ;;
esac