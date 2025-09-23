# Dockerfile
FROM openjdk:11-jre-slim
COPY target/DUKEAi-1.0.0.jar /app/
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/DUKEAi-1.0.0.jar"]