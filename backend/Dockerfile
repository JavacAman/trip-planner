# ── Stage 1: Build ──────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Cache Maven dependencies before copying source (layer caching)
COPY pom.xml .
RUN mvn dependency:go-offline -q

COPY src ./src
RUN mvn clean package -DskipTests -q

# ── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=builder /app/target/trip-planner-*.jar app.jar

RUN chown appuser:appgroup app.jar
USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

# Conservative memory tuning for small-memory hosting plans (e.g. Railway free/hobby tier):
# heap capped at 50% of the container limit (not 75%) leaves headroom for metaspace,
# thread stacks, and native buffers (Netty/Lettuce, Tomcat NIO) that live off-heap —
# without this, the container gets OOM-killed by the host under modest load.
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=50.0", \
            "-XX:MaxMetaspaceSize=128m", "-Xss512k", \
            "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
