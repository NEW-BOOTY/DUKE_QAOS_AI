# Copyright © 2025 Devin B. Royal. All Rights Reserved.
FROM eclipse-temurin:11-jre-alpine

# Install Bouncy Castle dependencies
RUN apk add --no-cache \
    ca-certificates \
    tzdata \
    && update-ca-certificates

# Create dukeai user
RUN addgroup -g 1001 dukeai && \
    adduser -D -s /bin/sh -u 1001 -G dukeai dukeai

# Create directories
RUN mkdir -p /opt/dukeai/logs /opt/dukeai/data && \
    chown dukeai:dukeai /opt/dukeai -R

# Copy application
COPY target/DUKEAi-1.0.0.jar /opt/dukeai/
RUN chown dukeai:dukeai /opt/dukeai/DUKEAi-1.0.0.jar

# Create startup script
RUN echo '#!/bin/sh\n\
set -e\n\
\n\
# Wait for dependencies\n\
sleep 5\n\
\n\
# JVM optimizations\n\
JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"\n\
\n\
# Run as dukeai user\n\
exec su-exec dukeai:dukeai java $JAVA_OPTS \\\n\
    -Djava.security.egd=file:/dev/./urandom \\\n\
    -Dfile.encoding=UTF-8 \\\n\
    -Dlogging.level.com.devinroyal.dukeai=INFO \\\n\
    -Dlogging.file.name=/opt/dukeai/logs/dukeai.log \\\n\
    -jar /opt/dukeai/DUKEAi-1.0.0.jar' > /opt/dukeai/start.sh && \
    chmod +x /opt/dukeai/start.sh && \
    chown dukeai:dukeai /opt/dukeai/start.sh

WORKDIR /opt/dukeai
USER dukeai

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8080/health || exit 1

ENTRYPOINT ["/opt/dukeai/start.sh"]