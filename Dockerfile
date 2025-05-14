FROM gradle:8.8-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle shadowJar --no-daemon

FROM eclipse-temurin:21.0.4_7-jre-jammy
WORKDIR /app
COPY --from=build /app/build/libs/discord-noriskfaqbot.jar ./app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]