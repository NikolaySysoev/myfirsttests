#!/bin/bash
# Полный прогон тестов: поднимает стенд через Docker Compose, запускает тесты
# в контейнере и гасит стенд — даже если тесты упали или прогон прерван (Ctrl+C).
# Использование: ./run-tests-with-docker-compose.sh [all|api|ui]   (по умолчанию all)
set -euo pipefail

cd "$(dirname "$0")" # запускать можно из любой папки

# Настройка
COMPOSE_FILE=infra/docker-compose.yml
BROWSERS_FILE=infra/config/browsers.json
IMAGE_NAME=nbank-tests
TEST_PROFILE=${1:-all}
TIMESTAMP=$(date +"%Y%m%d_%H%M")
TEST_OUTPUT_DIR="$(pwd)/test-output/$TIMESTAMP" # docker -v надёжно работает только с абсолютным путём
NETWORK=nbank-network
FRAUD_MOCK_PORT=8089 # бэк ищет фрод-мок на host.docker.internal:8089
WAIT_TIMEOUT=120     # сколько секунд ждём готовности стенда

# Адреса внутри docker-сети: тесты и браузеры Selenoid живут в nbank-network,
# localhost внутри контейнера — это сам контейнер, поэтому ходим по именам сервисов.
APIBASEURL_V1=${APIBASEURL_V1:-http://backend-legacy:4111}
APIBASEURL_V2=${APIBASEURL_V2:-http://backend:4111}
DBURL_V2=${DBURL_V2:-jdbc:postgresql://postgres:5432/nbank}
UIBASEURL=${UIBASEURL:-http://nginx}
UIREMOTE=${UIREMOTE:-http://selenoid:4444/wd/hub}

log() { echo ">>> [$(date +%H:%M:%S)] $*"; }

# Остановка стенда при любом выходе: успех, падение тестов, ошибка скрипта, Ctrl+C.
# -v удаляет том БД: каждый прогон начинается на чистой базе.
cleanup() {
  log "Остановка окружения"
  docker compose -f "$COMPOSE_FILE" down -v --remove-orphans || true
}
trap cleanup EXIT

# Ждём, пока по адресу начнут отвечать (любой HTTP-ответ, даже 401/404, значит сервис поднялся)
wait_for() {
  local name=$1 url=$2 elapsed=0
  printf "    %-15s %s " "$name" "$url"
  until curl -s -o /dev/null --max-time 2 "$url"; do
    if (( elapsed >= WAIT_TIMEOUT )); then
      echo "— не поднялся за ${WAIT_TIMEOUT}с"
      docker compose -f "$COMPOSE_FILE" ps
      exit 1
    fi
    sleep 2; elapsed=$((elapsed + 2)); printf "."
  done
  echo " OK"
}

# 1. Образы браузеров: Selenoid сам их не скачивает, без них сессия не создастся
log "Скачивание образов браузеров из $BROWSERS_FILE"
for image in $(grep -o '"image": *"[^"]*"' "$BROWSERS_FILE" | cut -d'"' -f4); do
  docker pull -q "$image"
done

# 2. Стенд. Предварительный down — на случай, если остался прошлый незавершённый прогон
log "Поднятие окружения ($COMPOSE_FILE)"
docker compose -f "$COMPOSE_FILE" down -v --remove-orphans > /dev/null 2>&1 || true
docker compose -f "$COMPOSE_FILE" up -d

log "Ожидание готовности сервисов (до ${WAIT_TIMEOUT}с)"
wait_for backend        http://localhost:4111
wait_for backend-legacy http://localhost:4112
wait_for nginx          http://localhost:80
wait_for selenoid       http://localhost:4444/status

# 3. Тесты
log "Сборка образа с тестами"
docker build -q -t "$IMAGE_NAME" . > /dev/null

mkdir -p "$TEST_OUTPUT_DIR/logs" "$TEST_OUTPUT_DIR/results" "$TEST_OUTPUT_DIR/report"

log "Запуск тестов (профиль: $TEST_PROFILE), лог: $TEST_OUTPUT_DIR/logs/run.log"
set +e # падение тестов не должно обрывать скрипт — сначала покажем итоги
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
  -e UIREMOTE="$UIREMOTE" \
  "$IMAGE_NAME"
TEST_EXIT_CODE=$?
set -e

# 4. Итоги
log "Тесты завершены"
grep -E "^\[(INFO|WARNING|ERROR)\] Tests run:" "$TEST_OUTPUT_DIR/logs/run.log" | tail -1 || true
echo "    Лог:        $TEST_OUTPUT_DIR/logs/run.log"
echo "    Результаты: $TEST_OUTPUT_DIR/results"
echo "    Отчёт:      $TEST_OUTPUT_DIR/report/surefire-report.html"
if (( TEST_EXIT_CODE == 0 )); then
  log "Все тесты прошли"
else
  log "Есть упавшие тесты (код $TEST_EXIT_CODE)"
fi

exit "$TEST_EXIT_CODE" # окружение остановит trap cleanup
