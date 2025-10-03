#!/bin/bash
SERVER="mindils@104.253.1.57"
LOCAL_PORT=4000
REMOTE_PORT=4000
PID_FILE="/tmp/ssh_tunnel_4000.pid"

start_tunnel() {
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if ps -p "$PID" > /dev/null 2>&1; then
            echo "❌ Туннель уже запущен (PID: $PID)"
            return 1
        else
            rm -f "$PID_FILE"
        fi
    fi

    # Убиваем все старые туннели на этом порту
    OLD_PIDS=$(pgrep -f "ssh.*[L].*${LOCAL_PORT}.*${SERVER}")
    if [ -n "$OLD_PIDS" ]; then
        echo "🧹 Останавливаем старые туннели: $OLD_PIDS"
        kill $OLD_PIDS 2>/dev/null
        sleep 1
    fi

    echo "🚀 Запускаем SSH туннель..."

    # Запускаем туннель и сразу получаем его PID
    ssh -g -f -N -L ${LOCAL_PORT}:localhost:${REMOTE_PORT} ${SERVER}
    SSH_EXIT=$?

    if [ $SSH_EXIT -ne 0 ]; then
        echo "❌ Ошибка запуска SSH (код: $SSH_EXIT)"
        return 1
    fi

    sleep 2

    # Ищем PID более надежным способом
    # Ищем SSH процесс с нужным портом и сервером
    PID=$(ps aux | grep "[s]sh.*${LOCAL_PORT}.*${SERVER}" | grep -v grep | awk '{print $2}' | head -n1)

    if [ -z "$PID" ]; then
        # Альтернативный поиск
        PID=$(pgrep -f "ssh.*${SERVER}" | while read p; do
            if ps -p $p -o args= | grep -q "${LOCAL_PORT}"; then
                echo $p
                break
            fi
        done)
    fi

    if [ -n "$PID" ]; then
        echo "$PID" > "$PID_FILE"
        echo "✅ Туннель запущен успешно (PID: $PID)"
        echo ""
        echo "📍 Локальный доступ: http://localhost:${LOCAL_PORT}"

        IP=$(hostname -I | awk '{print $1}')
        echo "🌍 Внешний доступ: http://${IP}:${LOCAL_PORT}"
        echo ""

        echo "🔌 Прослушивание портов:"
        sudo netstat -tlnp 2>/dev/null | grep ":${LOCAL_PORT}" || sudo ss -tlnp 2>/dev/null | grep ":${LOCAL_PORT}" || echo "Не удалось проверить (нужен sudo)"
    else
        echo "❌ Ошибка: не удалось найти PID запущенного туннеля"
        echo "Проверьте процессы вручную: ps aux | grep ssh"
        return 1
    fi
}

stop_tunnel() {
    STOPPED=0

    # Останавливаем по PID файлу
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if ps -p "$PID" > /dev/null 2>&1; then
            echo "🛑 Останавливаем туннель из PID файла (PID: $PID)..."
            kill "$PID"
            STOPPED=1
        fi
        rm -f "$PID_FILE"
    fi

    # Дополнительно ищем и убиваем все туннели на этом порту
    RUNNING_PIDS=$(ps aux | grep "[s]sh.*${LOCAL_PORT}.*${SERVER}" | awk '{print $2}')

    if [ -n "$RUNNING_PIDS" ]; then
        echo "🛑 Найдены дополнительные туннели: $RUNNING_PIDS"
        for PID in $RUNNING_PIDS; do
            echo "   Останавливаем PID: $PID"
            kill "$PID" 2>/dev/null
            STOPPED=1
        done
    fi

    if [ $STOPPED -eq 1 ]; then
        sleep 1
        echo "✅ Туннель остановлен"
    else
        echo "❌ Туннель не был запущен"
        return 1
    fi
}

status_tunnel() {
    FOUND=0

    # Проверяем PID файл
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if ps -p "$PID" > /dev/null 2>&1; then
            echo "✅ Туннель работает (PID из файла: $PID)"
            FOUND=1
        else
            echo "⚠️  PID файл существует, но процесс $PID не найден"
            rm -f "$PID_FILE"
        fi
    fi

    # Ищем все туннели на этом порту
    RUNNING_PIDS=$(ps aux | grep "[s]sh.*${LOCAL_PORT}.*${SERVER}" | awk '{print $2}')

    if [ -n "$RUNNING_PIDS" ]; then
        if [ $FOUND -eq 0 ]; then
            echo "⚠️  Найдены работающие туннели без PID файла:"
        fi
        for PID in $RUNNING_PIDS; do
            echo "   PID: $PID - $(ps -p $PID -o args= | head -c 100)"
            FOUND=1
        done
    fi

    if [ $FOUND -eq 0 ]; then
        echo "❌ Туннель не запущен"
        return 1
    fi

    echo ""
    IP=$(hostname -I | awk '{print $1}')
    echo "📍 Локальный доступ: http://localhost:${LOCAL_PORT}"
    echo "🌍 Внешний доступ: http://${IP}:${LOCAL_PORT}"
    echo ""

    echo "🔌 Прослушивание портов:"
    sudo netstat -tlnp 2>/dev/null | grep ":${LOCAL_PORT}" || sudo ss -tlnp 2>/dev/null | grep ":${LOCAL_PORT}" || echo "Не удалось проверить (нужен sudo)"
    echo ""

    if command -v curl > /dev/null 2>&1; then
        echo "🧪 Тестирование доступности..."

        if timeout 3 curl -s -o /dev/null -w "%{http_code}" http://localhost:${LOCAL_PORT} 2>&1 | grep -q "200\|301\|302\|000"; then
            echo "✅ Локально (localhost): доступен"
        else
            echo "⚠️  Локально (localhost): не отвечает"
        fi

        if timeout 3 curl -s -o /dev/null -w "%{http_code}" http://${IP}:${LOCAL_PORT} 2>&1 | grep -q "200\|301\|302\|000"; then
            echo "✅ По IP (${IP}): доступен"
        else
            echo "⚠️  По IP (${IP}): не доступен"
        fi
    fi
}

restart_tunnel() {
    echo "🔄 Перезапускаем туннель..."
    stop_tunnel
    sleep 2
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