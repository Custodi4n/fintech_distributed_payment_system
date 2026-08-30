# ==========================================
# Stage 1: Build JAR with Maven & OpenJDK 21
# ==========================================
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /build

COPY pom.xml .
COPY common-dto/pom.xml common-dto/
COPY account-service/pom.xml account-service/
COPY anti-fraud-service/pom.xml anti-fraud-service/
COPY payment-service/pom.xml payment-service/

RUN mvn dependency:go-offline -B -pl payment-service -am

COPY common-dto/src common-dto/src
COPY payment-service/src payment-service/src

RUN mvn clean package -DskipTests -pl payment-service -am

# ==========================================
# Stage 2: Minimal Runtime
# ==========================================
FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY --from=builder /build/payment-service/target/*.jar app.jar

RUN chown -R appuser:appgroup /app
USER appuser

EXPOSE 8083

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]