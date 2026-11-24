FROM eclipse-temurin:17-jdk AS build
WORKDIR /src
# Wrapper and build files first so the dependency download is cached
# until the build config actually changes.
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
RUN ./gradlew --no-daemon dependencies > /dev/null
COPY src src
RUN ./gradlew --no-daemon shadowJar

FROM eclipse-temurin:17-jre
# The bot opens "memories_database" relative to its working directory,
# so /data is the working directory and the volume mount point.
WORKDIR /data
COPY --from=build /src/build/libs/*-all.jar /app/bot.jar
VOLUME /data
# Main class is named explicitly because build.gradle.kts sets mainClass to
# the package name, which leaves the jar manifest without a runnable entry.
CMD ["java", "-cp", "/app/bot.jar", "org.github.SilverNoir4434.Main"]
