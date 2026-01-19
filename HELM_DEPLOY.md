# ResumeOpt Helm Deployment Guide

This guide details how to deploy ResumeOpt using Helm and troubleshoot scraping issues in a Kubernetes environment.

## Prerequisites

- Kubernetes Cluster (v1.20+)
- Helm 3.x installed
- Docker installed (for building the image)

## Quick Start

1. **Build the Docker Image**
   ```bash
   docker build -t resumeopt:latest .
   # If using Minikube:
   # eval $(minikube docker-env)
   # docker build -t resumeopt:latest .
   ```

2. **Deploy with Helm**
   ```bash
   cd k8s/helm
   helm install resumeopt .
   ```

3. **Verify Deployment**
   ```bash
   kubectl get pods
   kubectl logs -f deployment/resumeopt
   ```

## Configuration for Scraping

The `values.yaml` file contains specific configurations for job scraping:

```yaml
application:
  scraping:
    enabled: true
    proxy:
      enabled: false  # Set to true if behind a corporate proxy
      host: "proxy.example.com"
      port: "8080"
```

### Environment Variables

Key feature flags can be toggled via `values.yaml` or `--set`:

- `JOB_PORTALS_ENHANCED_ENABLED`: Enable/Disable enhanced scrapers (default: `true`)
- `JOB_PORTALS_LINKEDIN_ENABLED`: Enable/Disable LinkedIn (default: `false` due to anti-bot)

## Troubleshooting Scraping Issues

If scraping is not working in the cluster, follow these steps:

### 1. Check Connectivity (Health Check)
We have implemented a custom health check that verifies outbound internet connectivity.

```bash
# Forward port to access actuator
kubectl port-forward svc/resumeopt 8080:8080

# Check health status
curl http://localhost:8080/actuator/health
```
Look for the `scrapingHealthIndicator` (or similar) component status. If it is `DOWN`, the pod cannot reach the internet.

### 2. Verify Egress / Network Policies
If the health check fails:
- Check if your cluster has `NetworkPolicy` blocking egress.
- Verify DNS resolution:
  ```bash
  kubectl exec -it <pod-name> -- curl -v https://www.google.com
  ```

### 3. Check Logs for Anti-Bot Blocking
View logs to see if scrapers are getting `403 Forbidden` or `429 Too Many Requests`.

```bash
kubectl logs -l app.kubernetes.io/name=resumeopt --tail=200 -f
```
Search for: `Cloudflare`, `Captcha`, `Access Denied`.

### 4. Proxy Configuration
If your cluster requires a proxy, update `values.yaml`:

```yaml
application:
  scraping:
    proxy:
      enabled: true
      host: "your-proxy-host"
      port: "8080"
```
This sets the standard Java proxy system properties (`-Dhttp.proxyHost`, etc.).

### 5. Persistent Data
The H2 database is stored in `/app/data`. A PersistentVolumeClaim (PVC) is created by default.
If you restart the pod and lose data, verify the PVC is bound:
```bash
kubectl get pvc
```

## Known Limitations
- **LinkedIn/Glassdoor:** These sites have aggressive anti-bot protection. Running from a cloud IP (AWS/GCP) usually results in immediate blocking. Use residential proxies if available (future enhancement).
- **Memory Usage:** Scrapers can be memory intensive. If the pod restarts with `OOMKilled`, increase memory limits in `values.yaml`:
  ```yaml
  resources:
    limits:
      memory: 2Gi
  ```
