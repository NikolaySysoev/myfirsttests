#!/bin/bash
# Публикация образа с тестами в Docker Hub.
# Использование:
#   export DOCKERHUB_USERNAME=<логин>
#   export DOCKERHUB_TOKEN=<access token из Docker Hub: Account settings -> Personal access tokens>
#   ./push-tests.sh [tag]        # по умолчанию tag=latest
set -euo pipefail

# Настройка
IMAGE_NAME=nbank-tests                          # локальный образ, который собирает run-tests.sh
DOCKERHUB_USERNAME=${DOCKERHUB_USERNAME:?Задайте DOCKERHUB_USERNAME}
TAG=${1:-latest}
REMOTE_IMAGE="$DOCKERHUB_USERNAME/$IMAGE_NAME:$TAG"

# Формат тега по правилам Docker: буквы, цифры, _ . -, не больше 128 символов, не с . или -
if ! [[ "$TAG" =~ ^[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}$ ]]; then
  echo "Ошибка: недопустимый тег '$TAG'" >&2
  exit 1
fi

# Токен берём только из окружения — в файлах и в истории команд его быть не должно
# Если переменной нет, а скрипт запущен из терминала — спрашиваем токен со скрытым вводом
if [ -z "${DOCKERHUB_TOKEN:-}" ] && [ -t 0 ]; then
  read -rsp "Docker Hub token: " DOCKERHUB_TOKEN
  echo
fi
if [ -z "${DOCKERHUB_TOKEN:-}" ]; then
  echo "Ошибка: не задана переменная DOCKERHUB_TOKEN" >&2
  exit 1
fi

# Образ должен быть собран заранее
if ! docker image inspect "$IMAGE_NAME" >/dev/null 2>&1; then
  echo "Ошибка: локальный образ '$IMAGE_NAME' не найден. Соберите его: docker build -t $IMAGE_NAME ." >&2
  exit 1
fi

# Логин: токен уходит в stdin, а не аргументом — так он не светится в ps и истории
echo ">>> Логин в Docker Hub как $DOCKERHUB_USERNAME"
echo "$DOCKERHUB_TOKEN" | docker login --username "$DOCKERHUB_USERNAME" --password-stdin

# Тегирование в формат <username>/<image>:<tag>
echo ">>> Тегирование: $IMAGE_NAME -> $REMOTE_IMAGE"
docker tag "$IMAGE_NAME" "$REMOTE_IMAGE"

# Пуш
echo ">>> Пуш $REMOTE_IMAGE"
docker push "$REMOTE_IMAGE"

echo ">>> Готово. Скачать образ:"
echo "docker pull $REMOTE_IMAGE"
