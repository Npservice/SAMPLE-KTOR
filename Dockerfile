# syntax=docker/dockerfile:1

# ---------- Build ----------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /src

# Download Gradle & dependency dulu, supaya layer ini ter-cache selama build file tidak berubah.
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
# gradlew bisa ber-line-ending CRLF kalau di-checkout di Windows.
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew \
    && ./gradlew --no-daemon --no-configuration-cache dependencies > /dev/null

COPY src ./src
RUN ./gradlew --no-daemon --no-configuration-cache installDist -x test

# ---------- Runtime ----------
FROM eclipse-temurin:25-jre
WORKDIR /app

RUN groupadd --system app && useradd --system --gid app --home-dir /app app

# bin/example (aplikasi) dan bin/seed, beserta semua jar di lib/.
COPY --from=build /src/build/install/example ./
RUN mkdir -p storage logs && chown -R app:app /app

USER app

# MaxRAMPercentage: ukuran heap mengikuti limit memori container.
ENV PORT=3000 \
    LOG_DIR=/app/logs \
    JAVA_OPTS="-XX:MaxRAMPercentage=75"

EXPOSE 3000

# Flag --enable-native-access sudah ada di dalam script bin/example (dari build.gradle.kts).
ENTRYPOINT ["./bin/example"]
