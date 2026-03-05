# ── Estágio 1: Build ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build

# Cache das dependências Maven antes de copiar o código
COPY pom.xml .
COPY lombok.config .
RUN apk add --no-cache maven && \
    mvn -B -q dependency:go-offline -DskipTests

COPY src ./src
RUN mvn -B -q package -DskipTests

# ── Estágio 2: Runtime ────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Usuário não-root por segurança
RUN addgroup -S etl && adduser -S etl -G etl && \
    mkdir -p /tmp/etl/camara /app/logs && \
    chown -R etl:etl /tmp/etl /app/logs

USER etl

COPY --from=builder /build/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
