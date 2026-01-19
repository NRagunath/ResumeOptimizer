# Rollback Procedures for ResumeOpt

In the event of a deployment failure or critical issue detected in production, follow these procedures to revert to a stable state.

## Trigger Conditions
Initiate a rollback if:
1.  **Health Check Failure**: `/actuator/health` returns `DOWN` or times out > 60s.
2.  **Elevated Error Rates**: Scraping failure rate exceeds 20% (monitored via `ScrapingMonitorService`).
3.  **Data Integrity Issues**: "Complete Job %" drops below 80%.
4.  **Critical Bugs**: Core functionality (Resume parsing, Job matching) is broken.

## Rollback Strategies

### 1. Standalone JAR Deployment
If running as a system service or standalone JAR:

1.  **Stop the current process**:
    ```bash
    kill <PID>
    # Windows
    Stop-Process -Id <PID>
    ```
2.  **Restore backup artifact**:
    The previous working JAR should be versioned or backed up (e.g., `resumeopt-0.0.9.jar`).
    ```bash
    cp backup/resumeopt-prev.jar target/resumeopt-current.jar
    ```
3.  **Restart Service**:
    ```bash
    java -jar target/resumeopt-current.jar
    ```

### 2. Kubernetes / Helm Deployment
Helm makes rollback trivial by tracking release history.

1.  **Check History**:
    ```bash
    helm history resumeopt
    ```
2.  **Rollback to Previous Revision**:
    ```bash
    # Rollback to the immediate previous version
    helm rollback resumeopt 0
    
    # Or rollback to specific revision (e.g., 1)
    helm rollback resumeopt 1
    ```
3.  **Verify Rollback**:
    Wait for pods to restart and become ready.
    ```bash
    kubectl get pods -w
    ```

### 3. Docker Deployment
1.  **Stop current container**:
    ```bash
    docker stop resumeopt
    docker rm resumeopt
    ```
2.  **Run previous tag**:
    ```bash
    docker run -d -p 8080:8080 --name resumeopt resumeopt:v0.0.9
    ```

## Post-Rollback Actions
1.  **Save Logs**: Archive logs from the failed deployment (`deployment.log`, `deployment_error.log`) for analysis.
2.  **Notify Team**: Alert stakeholders that a rollback occurred.
3.  **Root Cause Analysis**: Investigate why the new version failed before re-attempting deployment.
