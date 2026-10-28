param(
  [switch]$Strict
)

$ErrorActionPreference = "Stop"

function Write-Status {
  param(
    [string]$Name,
    [bool]$Ok,
    [string]$Detail = ""
  )
  $icon = if ($Ok) { "OK" } else { "WARN" }
  $color = if ($Ok) { "Green" } else { "Yellow" }
  Write-Host ("[{0}] {1}" -f $icon, $Name) -ForegroundColor $color -NoNewline
  if ($Detail) { Write-Host " - $Detail" } else { Write-Host "" }
  if ($Strict -and -not $Ok) { $script:hasError = $true }
}

function Test-Command {
  param(
    [string]$Command,
    [string]$Label,
    [string]$VersionArg = "--version"
  )
  $cmd = Get-Command $Command -ErrorAction SilentlyContinue
  if (-not $cmd) {
    Write-Status $Label $false "$Command not found"
    return
  }
  try {
    $version = (& $Command $VersionArg 2>&1 | Select-Object -First 1) -join " "
    Write-Status $Label $true $version
  } catch {
    Write-Status $Label $true $cmd.Source
  }
}

function Test-Port {
  param(
    [int]$Port,
    [string]$Label
  )
  $listener = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue | Select-Object -First 1
  if ($listener) {
    Write-Status $Label $true "port $Port is listening (pid $($listener.OwningProcess))"
  } else {
    Write-Status $Label $false "port $Port is not listening"
  }
}

function Test-EnvVar {
  param(
    [string]$Name,
    [bool]$Required = $false
  )
  $value = [Environment]::GetEnvironmentVariable($Name, "Process")
  if (-not $value) { $value = [Environment]::GetEnvironmentVariable($Name, "User") }
  if (-not $value) { $value = [Environment]::GetEnvironmentVariable($Name, "Machine") }
  $ok = -not [string]::IsNullOrWhiteSpace($value)
  $detail = if ($ok) { "configured" } elseif ($Required) { "missing" } else { "optional" }
  Write-Status "env:$Name" ($ok -or -not $Required) $detail
}

$script:hasError = $false

Write-Host "AI Interview Coach environment check" -ForegroundColor Cyan
Write-Host "Workspace: $((Get-Location).Path)"
Write-Host ""

Test-Command "java" "Java"
Test-Command "mvn" "Maven"
Test-Command "node" "Node.js"
Test-Command "npm" "npm"
Test-Command "mysql" "MySQL CLI"
Test-Command "redis-cli" "Redis CLI"
Test-Command "docker" "Docker" "version"

Write-Host ""
Test-EnvVar "DEEPSEEK_API_KEY" $true
Test-EnvVar "INTERVIEW_DB_PASSWORD" $true
Test-EnvVar "INTERVIEW_JWT_SECRET" $true
Test-EnvVar "INTERVIEW_CORS_ORIGINS" $false
Test-EnvVar "INTERVIEW_AI_TIMEOUT_SECONDS" $false

Write-Host ""
Test-Port 3306 "MySQL"
Test-Port 6379 "Redis"
Test-Port 8080 "Backend"
Test-Port 5173 "Frontend"

Write-Host ""
if ($script:hasError) {
  Write-Host "Strict check failed. Configure missing required items and rerun." -ForegroundColor Red
  exit 1
}

Write-Host "Check complete. WARN items may still be fine if you use Docker or start services later." -ForegroundColor Cyan
