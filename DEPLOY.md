# Deployment Documentation for ResumeOpt

## Overview
ResumeOpt is designed to be deployed as a containerized application (Docker/Kubernetes) or as a standalone Java JAR service. The application follows strict CI/CD standards with automated testing and health monitoring.

## Prerequisites
- **Java Runtime**: JDK 17+ (Eclipse Temurin recommended)
- **Build Tool**: Maven 3.6+
- **Container Engine**: Docker (Optional, for containerized deployment)
- **Orchestrator**: Kubernetes/Helm (Optional, for cluster deployment)

## Deployment Steps

### 1. Build Production Artifact
Ensure all tests pass before building.
```bash
# Run validation tests
mvn test -Dtest=ScraperValidationTest

# Build optimized JAR
mvn clean package -DskipTests
```
*Artifact Location*: `target/resumeopt-0.1.0.jar`

### 2. Environment Configuration
The application uses Spring Profiles. Set `SPRING_PROFILES_ACTIVE=prod` for production deployment.

**Key Environment Variables:**
| Variable | Description | Default |
| :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | Active profile (dev, prod) | `dev` |
| `SERVER_PORT` | HTTP Port | `8080` |
| `JOB_PORTALS_*_ENABLED` | Toggle specific scrapers | `true` |
| `JOB_PORTALS_*_REQUESTDELAY` | Rate limiting (ms) | `2000` |

### 3. Deployment Options

#### Option A: Standalone JAR (Linux/Windows)
Use the provided simulation script for verification or run manually:
```bash
export SPRING_PROFILES_ACTIVE=prod
java -jar target/resumeopt-0.1.0.jar
```
*Note: Use `deploy_prod_simulation.ps1` for a verified local start on Windows.*

#### Option B: Docker Container
```bash
docker build -t resumeopt:latest .
docker run -d -p 8080:8080 --env SPRING_PROFILES_ACTIVE=prod resumeopt:latest
```

#### Option C: Kubernetes (Helm)
```bash
helm upgrade --install resumeopt k8s/helm/ --values k8s/helm/values.yaml
```

## Monitoring & Validation
The application exposes Actuator endpoints for real-time monitoring:
- **Health Check**: `http://localhost:8080/actuator/health` (Returns `{"status":"UP"}`)
- **Liveness Probe**: `http://localhost:8080/actuator/health/liveness`
- **Readiness Probe**: `http://localhost:8080/actuator/health/readiness`
- **Metrics**: `http://localhost:8080/actuator/prometheus` (if enabled)

### Automated Verification
Run the deployment simulation script to verify the build health:
```powershell
./deploy_prod_simulation.ps1
```

## Security Compliance
- **Non-Root User**: Dockerfile is configured to run as `spring` user (UID 1000+).
- **Secrets Management**: Use K8s Secrets or Environment Variables for sensitive keys (DB credentials, API keys).
- **Probes**: Liveness/Readiness probes are configured in `deployment.yaml` to ensure zero-downtime updates.
