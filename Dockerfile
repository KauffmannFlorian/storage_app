# =========================
# Build stage
# =========================
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy sources and build
COPY src ./src
RUN mvn clean package -DskipTests

# =========================
# Runtime stage (optimized JRE)
# =========================
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy only the compiled JAR (application ≤ 200MB)
COPY --from=build /app/target/*.jar storage_app.jar

# Expose the default port
EXPOSE 8080

# Enforce 1GB memory max (runtime constraint)
ENV JAVA_OPTS="-Xmx1024m -XX:+UseG1GC -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar storage_app.jar"]
