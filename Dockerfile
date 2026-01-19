FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Add a non-root user
RUN groupadd -r spring && useradd -r -g spring spring

# Create data directory with permissions
RUN mkdir -p /app/data && chown -R spring:spring /app/data

# Copy jar file
COPY target/resumeopt-0.1.0.jar app.jar

# Set ownership
RUN chown spring:spring app.jar

# Switch to non-root user
USER spring

# Expose port
EXPOSE 8080

# Environment variables for scraping optimization
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
