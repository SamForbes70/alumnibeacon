# ============================================================
# AlumniBeacon - Single Container Build
# Spring Boot 3.4 + FastAPI August Adapter
# ============================================================

# Stage 1: Build Spring Boot
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn package -DskipTests -q

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-jammy

# Install Python + supervisor
RUN apt-get update && apt-get install -y \
    python3 python3-pip python3-venv \
    supervisor \
    && rm -rf /var/lib/apt/lists/*

# Create data directory for SQLite
RUN mkdir -p /data /app/osint-adapter

# Copy Spring Boot jar
COPY --from=builder /build/target/*.jar /app/app.jar

# Copy and install Python OSINT adapter
COPY osint-adapter/requirements.txt /app/osint-adapter/
RUN pip3 install --no-cache-dir -r /app/osint-adapter/requirements.txt
COPY osint-adapter/main.py /app/osint-adapter/

# Supervisor config
COPY docker/supervisord.conf /etc/supervisor/conf.d/supervisord.conf

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

CMD ["/usr/bin/supervisord", "-c", "/etc/supervisor/conf.d/supervisord.conf"]
