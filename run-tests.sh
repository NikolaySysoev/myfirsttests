#!/bin/bash
# Прогон тестов в Docker против локального стенда из infra/docker-compose.yml.
# Стенд должен быть поднят: docker compose -f infra/docker-compose.yml up -d
# Использование: ./run-tests.sh [api|ui]
set -euo pipefail

# Настройка
IMAGE_NAME=nbank-tests
TEST_PROFILE=${1:-api} # аргумент запуска
TIMESTAMP=$(date +"%Y%m%d_%H%M")
TEST_OUTPUT_DIR="$(pwd)/test-output/$TIMESTAMP" # docker -v надёжно работает только с абсолютным путём

NETWORK=nbank-network
# Бэк ищет фрод-сервис на host.docker.internal:8089 — пробрасываем туда WireMock из контейнера
FRAUD_MOCK_PORT=8089

# Адреса внутри docker-сети: имена сервисов из compose и их внутренние порты.
# Любой можно переопределить переменной окружения при запуске скрипта.
APIBASEURL_V1=${APIBASEURL_V1:-http://backend-legacy:4111}
APIBASEURL_V2=${APIBASEURL_V2:-http://backend:4111}
DBURL_V2=${DBURL_V2:-jdbc:postgresql://postgres:5432/nbank}
UIBASEURL=${UIBASEURL:-http://localhost:3000}

# Собираем Docker образ
echo ">>> Сборка тестов запущена"
docker build -t "$IMAGE_NAME" .

mkdir -p "$TEST_OUTPUT_DIR/logs" "$TEST_OUTPUT_DIR/results" "$TEST_OUTPUT_DIR/report"

# Запуск Docker контейнера
echo ">>> Тесты запущены (профиль: $TEST_PROFILE)"
docker run --rm \
  --network "$NETWORK" \
  -p "$FRAUD_MOCK_PORT:$FRAUD_MOCK_PORT" \
  -v "$TEST_OUTPUT_DIR/logs":/app/logs \
  -v "$TEST_OUTPUT_DIR/results":/app/target/surefire-reports \
  -v "$TEST_OUTPUT_DIR/report":/app/target/site \
  -e TEST_PROFILE="$TEST_PROFILE" \
  -e APIBASEURL_V1="$APIBASEURL_V1" \
  -e APIBASEURL_V2="$APIBASEURL_V2" \
  -e DBURL_V2="$DBURL_V2" \
  -e UIBASEURL="$UIBASEURL" \
  "$IMAGE_NAME"

# Вывод итогов
echo ">>> Тесты завершены"
echo "Лог файл: $TEST_OUTPUT_DIR/logs/run.log"
echo "Результаты тестов: $TEST_OUTPUT_DIR/results"
echo "Репорт: $TEST_OUTPUT_DIR/report"
