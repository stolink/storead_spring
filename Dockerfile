# Runtime stage - GitHub Actions에서 빌드된 JAR만 복사
# Multi-stage 빌드 제거: Maven Central rate limiting 회피 및 빌드 캐시 활용
FROM amazoncorretto:21-alpine

WORKDIR /app

# Install curl for healthcheck
RUN apk add --no-cache curl

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring

# Create storage directory
RUN mkdir -p /app/storage/uploads && chown -R spring:spring /app

# Copy pre-built jar file from context (built by GitHub Actions)
COPY --chown=spring:spring build/libs/sto-read-backend-0.0.1-SNAPSHOT.jar app.jar

# Switch to non-root user
USER spring:spring

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
