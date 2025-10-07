#!/usr/bin/env python3
"""
Веб-интерфейс для развертывания JB2
Запуск: python3 web-deploy.py
URL: http://your-server:5000
"""

from flask import Flask, jsonify, render_template_string
import subprocess
import threading
import os
import time
from datetime import datetime

app = Flask(__name__)

# Глобальное состояние
deploy_status = {
    'running': False,
    'last_start': None,
    'last_finish': None,
    'last_status': None,
    'output': [],
    'error': None
}

SCRIPT_PATH = os.path.join(os.path.dirname(__file__), 'deploy.sh')

def run_deploy():
    """Запуск скрипта развертывания в фоне"""
    global deploy_status

    deploy_status['running'] = True
    deploy_status['last_start'] = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
    deploy_status['output'] = []
    deploy_status['error'] = None

    try:
        process = subprocess.Popen(
            ['bash', SCRIPT_PATH],
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            bufsize=1
        )

        # Читаем вывод построчно
        for line in process.stdout:
            deploy_status['output'].append(line.strip())
            # Оставляем только последние 100 строк
            if len(deploy_status['output']) > 100:
                deploy_status['output'].pop(0)

        process.wait()

        if process.returncode == 0:
            deploy_status['last_status'] = 'success'
        else:
            deploy_status['last_status'] = 'failed'
            deploy_status['error'] = f'Exit code: {process.returncode}'

    except Exception as e:
        deploy_status['last_status'] = 'error'
        deploy_status['error'] = str(e)
    finally:
        deploy_status['running'] = False
        deploy_status['last_finish'] = datetime.now().strftime('%Y-%m-%d %H:%M:%S')

@app.route('/')
def index():
    """Главная страница"""
    html = """
    <!DOCTYPE html>
    <html>
    <head>
        <title>JB2 Deploy</title>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <style>
            body { 
                font-family: Arial, sans-serif; 
                max-width: 800px; 
                margin: 50px auto; 
                padding: 20px;
                background: #f5f5f5;
            }
            .container {
                background: white;
                padding: 30px;
                border-radius: 8px;
                box-shadow: 0 2px 4px rgba(0,0,0,0.1);
            }
            h1 { color: #333; margin-top: 0; }
            .btn {
                display: inline-block;
                padding: 12px 24px;
                margin: 10px 5px;
                background: #007bff;
                color: white;
                text-decoration: none;
                border-radius: 4px;
                font-size: 16px;
            }
            .btn:hover { background: #0056b3; }
            .btn.secondary { background: #6c757d; }
            .btn.secondary:hover { background: #545b62; }
            .status {
                margin: 20px 0;
                padding: 15px;
                border-radius: 4px;
                background: #e9ecef;
            }
            .running { background: #fff3cd; border-left: 4px solid #ffc107; }
            .success { background: #d4edda; border-left: 4px solid #28a745; }
            .error { background: #f8d7da; border-left: 4px solid #dc3545; }
            pre {
                background: #f8f9fa;
                padding: 15px;
                border-radius: 4px;
                overflow-x: auto;
                font-size: 12px;
            }
            .info { color: #666; font-size: 14px; }
        </style>
    </head>
    <body>
        <div class="container">
            <h1>🚀 JB2 Deployment</h1>
            
            <div>
                <a href="/deploy" class="btn">Запустить развертывание</a>
                <a href="/status" class="btn secondary">Статус (JSON)</a>
                <a href="/" class="btn secondary">Обновить</a>
            </div>
            
            <div class="status {{ status_class }}">
                <strong>Статус:</strong> {{ status_text }}<br>
                {% if last_start %}
                <strong>Последний запуск:</strong> {{ last_start }}<br>
                {% endif %}
                {% if last_finish %}
                <strong>Завершено:</strong> {{ last_finish }}<br>
                {% endif %}
                {% if error %}
                <strong>Ошибка:</strong> {{ error }}<br>
                {% endif %}
            </div>
            
            {% if output %}
            <h3>Вывод (последние строки):</h3>
            <pre>{{ output }}</pre>
            {% endif %}
            
            <div class="info">
                <p><strong>API:</strong></p>
                <ul>
                    <li><code>GET /deploy</code> - Запустить развертывание</li>
                    <li><code>GET /status</code> - Получить статус (JSON)</li>
                </ul>
                <p><strong>Примеры curl:</strong></p>
                <pre>curl http://localhost:5000/deploy
curl http://localhost:5000/status</pre>
            </div>
        </div>
    </body>
    </html>
    """

    status_class = ''
    status_text = 'Ожидание'

    if deploy_status['running']:
        status_class = 'running'
        status_text = 'Выполняется развертывание...'
    elif deploy_status['last_status'] == 'success':
        status_class = 'success'
        status_text = 'Успешно завершено'
    elif deploy_status['last_status'] in ('failed', 'error'):
        status_class = 'error'
        status_text = 'Ошибка'

    output_text = '\n'.join(deploy_status['output'][-20:]) if deploy_status['output'] else ''

    return render_template_string(
        html,
        status_class=status_class,
        status_text=status_text,
        last_start=deploy_status['last_start'],
        last_finish=deploy_status['last_finish'],
        error=deploy_status['error'],
        output=output_text
    )

@app.route('/deploy')
def deploy():
    """Запуск развертывания через GET"""
    if deploy_status['running']:
        return jsonify({
            'status': 'error',
            'message': 'Развертывание уже выполняется'
        }), 409

    # Запуск в отдельном потоке
    thread = threading.Thread(target=run_deploy)
    thread.daemon = True
    thread.start()

    return jsonify({
        'status': 'started',
        'message': 'Развертывание запущено',
        'started_at': deploy_status['last_start']
    })

@app.route('/status')
def status():
    """Получение статуса"""
    return jsonify({
        'running': deploy_status['running'],
        'last_start': deploy_status['last_start'],
        'last_finish': deploy_status['last_finish'],
        'last_status': deploy_status['last_status'],
        'error': deploy_status['error'],
        'output_lines': len(deploy_status['output']),
        'last_output': deploy_status['output'][-10:] if deploy_status['output'] else []
    })

if __name__ == '__main__':
    print("=" * 50)
    print("JB2 Web Deploy")
    print("=" * 50)
    print(f"URL: http://localhost:5000")
    print(f"Deploy: http://localhost:5000/deploy")
    print(f"Status: http://localhost:5000/status")
    print("=" * 50)

    # Запуск на всех интерфейсах (доступно извне)
    app.run(host='0.0.0.0', port=5000, debug=False)