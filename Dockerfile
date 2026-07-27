# syntax=docker/dockerfile:1
# Requires an additional build context pointing at the lora-codecs checkout:
#   docker build --build-context lora-codecs=../lora-codecs -t ioc-building .
# (the compose file wires this up via additional_contexts)

# Stage 1: compile the lora-codecs library from source. Its own Gradle 5.1
# build does not run on modern JDKs and the old snapshot repository is gone,
# but the library has no runtime dependencies so javac + jar is sufficient.
FROM eclipse-temurin:21-jdk AS codecs
WORKDIR /codecs
COPY --from=lora-codecs src/main/java src
RUN find src -name '*.java' > sources.txt \
    && javac --release 11 -d classes @sources.txt \
    && jar cf lora-codecs-0.0.1-SNAPSHOT.jar -C classes .

# Stage 2: build the Spring Boot jar with the Gradle wrapper.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradlew ./
COPY gradle gradle
COPY settings.gradle build.gradle ./
COPY --from=codecs /codecs/lora-codecs-0.0.1-SNAPSHOT.jar libs/
COPY src src
RUN ./gradlew --no-daemon bootJar

# Stage 3: slim runtime.
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/ioc-building-*.jar app.jar
# The compose stack sets LOGGING_FILE_NAME=/var/log/ioc-building/app.log and
# shares this directory with the log-monitor microservice.
RUN mkdir -p /var/log/ioc-building
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
