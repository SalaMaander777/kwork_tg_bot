# syntax=docker/dockerfile:1

# ---- сборка: Maven + админка (frontend-maven-plugin сам ставит Node в target/) ----
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Сначала только файлы зависимостей, чтобы слой кэшировался
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline -Pskip-ui || true

COPY admin-ui admin-ui
COPY src src
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q package -DskipTests

# ---- рантайм ----
FROM eclipse-temurin:17-jre
WORKDIR /app

RUN useradd --system --uid 1001 bot
COPY --from=build /app/target/TelegramKworkBot-*.jar app.jar
USER bot

EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -Dfile.encoding=UTF-8 -Duser.timezone=Europe/Moscow"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
