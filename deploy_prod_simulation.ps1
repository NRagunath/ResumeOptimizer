# Deployment Simulation Script for ResumeOpt
# Simulates Production Environment locally

$ErrorActionPreference = "Stop"

$JAR_PATH = "target/resumeopt-0.1.0.jar"
$LOG_FILE = "deployment.log"
$ERR_FILE = "deployment_error.log"
$PID_FILE = "app.pid"

Write-Host "Starting ResumeOpt Deployment Simulation..." -ForegroundColor Cyan

# 1. Pre-Deployment Checks
if (-not (Test-Path $JAR_PATH)) {
    Write-Error "Build artifact not found at $JAR_PATH. Please run 'mvn clean package' first."
}

# 2. Environment Configuration (Matching Helm values)
$env:SPRING_PROFILES_ACTIVE = "prod"
$env:SERVER_PORT = "8080"
# Add other env vars from Helm if needed
# $env:JOB_PORTALS_LINKEDIN_ENABLED = "true" 

Write-Host "Configuration:"
Write-Host "  Profile: $env:SPRING_PROFILES_ACTIVE"
Write-Host "  Port: $env:SERVER_PORT"
Write-Host "  Artifact: $JAR_PATH"

# 3. Start Application
Write-Host "Starting application..."
$process = Start-Process -FilePath "java" -ArgumentList "-jar", $JAR_PATH -RedirectStandardOutput $LOG_FILE -RedirectStandardError $ERR_FILE -PassThru -NoNewWindow

$process.Id | Out-File $PID_FILE
Write-Host "Application started with PID $($process.Id). Logs: $LOG_FILE, Errors: $ERR_FILE"

# 4. Health Check Loop
$maxRetries = 30
$retryCount = 0
$healthy = $false
$healthUrl = "http://localhost:8080/actuator/health"

Write-Host "Waiting for health check at $healthUrl..."

while ($retryCount -lt $maxRetries) {
    Start-Sleep -Seconds 2
    try {
        $response = Invoke-RestMethod -Uri $healthUrl -Method Get -ErrorAction SilentlyContinue
        if ($response.status -eq "UP") {
            $healthy = $true
            break
        }
    } catch {
        Write-Host "." -NoNewline
    }
    $retryCount++
}

Write-Host "" # Newline

# 5. Verification Result
if ($healthy) {
    Write-Host "✅ DEPLOYMENT SUCCESSFUL" -ForegroundColor Green
    Write-Host "System Status: UP"
    
    # Check Liveness/Readiness explicitly as per K8s
    try {
        $liveness = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health/liveness"
        $readiness = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health/readiness"
        Write-Host "Liveness Probe: $($liveness.status)"
        Write-Host "Readiness Probe: $($readiness.status)"
    } catch {
        Write-Warning "Could not query specific probes, but main health is UP."
    }

    Write-Host "Application is running. Use 'Stop-Process -Id $($process.Id)' to stop it."
} else {
    Write-Host "❌ DEPLOYMENT FAILED" -ForegroundColor Red
    Write-Host "Health check timed out."
    Write-Host "Checking logs..."
    Get-Content $LOG_FILE -Tail 20
    
    Stop-Process -Id $process.Id -Force
    Remove-Item $PID_FILE -ErrorAction SilentlyContinue
    exit 1
}
