# ==========================================
# Stage 1: Build JAR with Maven & OpenJDK 21
# ==========================================
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Копируем корневой POM и POM всех модулей для кэширования зависимостей
COPY pom.xml .
COPY common-dto/pom.xml common-dto/
COPY account-service/pom.xml account-service/
COPY anti-fraud-service/pom.xml anti-fraud-service/
COPY payment-service/pom.xml payment-service/

# Скачиваем зависимости (кэшируемый слой)
RUN mvn dependency:go-offline -B -pl account-service -am

# Копируем исходный код common-dto и целевого сервиса
COPY common-dto/src common-dto/src
COPY account-service/src account-service/src

# Собираем fat JAR
RUN mvn clean package -DskipTests -pl account-service -am

# ==========================================
# Stage 2: Minimal Distroless / Alpine Runtime
# ==========================================
FROM eclipse-temurin:21-jre-alpine

# Создаем непривилегированного пользователя в целях безопасности
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Копируем скомпилированный JAR
COPY --from=builder /build/account-service/target/*.jar app.jar

RUN chown -R appuser:appgroup /app
USER appuser

EXPOSE 8081

# Оптимизированные JVM флаги для контейнеров
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]