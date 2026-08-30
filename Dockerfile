# ===== Giai doan 1: Build jar bang Maven + JDK 17 =====
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom truoc de tan dung cache layer khi dependencies khong doi
COPY pom.xml .
RUN mvn -q dependency:go-offline -B

# Copy source va build (bo qua test cho nhanh)
COPY src ./src
RUN mvn -q clean package -DskipTests -B

# ===== Giai doan 2: Chay app bang JRE 17 (image nhe) =====
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy jar da build tu giai doan tren
COPY --from=build /app/target/vlxd-anhvu-1.0.0.jar app.jar

# Railway cap PORT qua bien moi truong; Spring doc SERVER_PORT
ENV JAVA_OPTS=""
EXPOSE 8095

# Chay app; PORT do Railway inject se duoc map vao server.port
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT:-8095} -jar app.jar"]
