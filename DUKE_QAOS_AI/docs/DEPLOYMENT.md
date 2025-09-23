Production Deployment
# Dockerfile
FROM openjdk:11-jre-slim
COPY target/DUKEAi-1.0.0.jar /app/
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/DUKEAi-1.0.0.jar"]
Kubernetes
# deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: duke-qaos-ai
spec:
  replicas: 3
  selector:
    matchLabels:
      app: duke-qaos-ai
  template:
    metadata:
      labels:
        app: duke-qaos-ai
    spec:
      containers:
      - name: duke-qaos-ai
        image: duke-qaos-ai:latest
        ports:
        - containerPort: 8080
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
---
apiVersion: v1
kind: Service
metadata:
  name: duke-qaos-ai-service
spec:
  selector:
    app: duke-qaos-ai
  ports:
    - protocol: TCP
      port: 80
      targetPort: 8080
  type: LoadBalancer
  
API Endpoints
Endpoint,Method,Description,Security
/,GET,Single-page dashboard,None
/events,GET,Server-Sent Events (logs),SSE
/api/process-task,POST,Quantum task processing,Input validation
/api/monitor-security,POST,Security event monitoring,Threat detection
/api/register-user,POST,User registration with MFA,PQ signatures
/api/verify-user,POST,MFA verification,PQ verification
/api/secure-exchange,POST,Encrypted message exchange,AES-GCM + ML-KEM
/api/performance,GET,Performance metrics,None
/api/logs,GET,Recent system logs,Log sanitization