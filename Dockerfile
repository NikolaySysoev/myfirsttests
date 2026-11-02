FROM maven:3.9.9-eclipse-temurin-21

# Дефолтные значения аргументов
ARG TEST_PROFILE=api
ARG APIBASEURL_V1=http://localhost:4112
ARG APIBASEURL_V2=http://localhost:4111
ARG UIBASEURL=http://localhost:3000
ARG DBURL_V2=jdbc:postgresql://localhost:5433/nbank

# Переменные окружения для контейнера
ENV TEST_PROFILE=${TEST_PROFILE}
ENV APIBASEURL_V1=${APIBASEURL_V1}
ENV APIBASEURL_V2=${APIBASEURL_V2}
ENV UIBASEURL=${UIBASEURL}
ENV DBURL_V2=${DBURL_V2}

# работаем из папки /app
WORKDIR /app

# копируем помник
COPY pom.xml .

# загружаем зависимости и кешируем
RUN mvn dependency:go-offline

# копируем весь проект
COPY . .

# теперь внутри есть зависимости, есть весь проект и мы готовы запускать тесты

USER root

# mvn test -P api
# mvn -DskipTests=true surfire-report:report
# лог выводился не в консаль, а в файл
# bash file
CMD /bin/bash -c " \
    mkdir -p /app/logs ; \
    { \
    echo '>>> Running tests with profile: ${TEST_PROFILE}' ; \
    mvn test -P ${TEST_PROFILE} ; \
    \
    echo '>>> Running surefire-report:report' ; \
    mvn -DskipTests=true surefire-report:report ; \
   } > /app/logs/run.log 2>&1"