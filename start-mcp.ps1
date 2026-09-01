$ErrorActionPreference = "Stop"

$env:AURORA_MCP_SERVER_ENABLED = "true"
$env:AURORA_MCP_BEARER_TOKEN = "aurora-demo-token"

$jdkCandidates = @(
    (Join-Path $env:USERPROFILE ".jdks\ms-21.0.11"),
    (Join-Path $env:USERPROFILE ".jdks\openjdk-24.0.2+12-54")
)
$env:JAVA_HOME = $jdkCandidates |
    Where-Object { Test-Path -LiteralPath "$_\bin\java.exe" } |
    Select-Object -First 1

if (-not (Test-Path -LiteralPath "$env:JAVA_HOME\bin\java.exe")) {
    throw "JAVA_HOME does not point to a valid JDK: $env:JAVA_HOME"
}

$maven = Get-ChildItem -Path "$env:USERPROFILE\.m2\wrapper\dists" -Recurse -Filter "mvn.cmd" -ErrorAction SilentlyContinue |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1 -ExpandProperty FullName

if (-not $maven) {
    throw "Maven was not found under $env:USERPROFILE\.m2\wrapper\dists"
}

Write-Host "Starting AURORA MCP server..."
Write-Host "Bearer token: $env:AURORA_MCP_BEARER_TOKEN"
Write-Host "Maven: $maven"

Write-Host "Building backend modules..."
& $maven -pl aurora-server -am -DskipTests package
if ($LASTEXITCODE -ne 0) {
    throw "Backend build failed with exit code $LASTEXITCODE"
}

Write-Host "Starting aurora-server with MCP profile..."
& $maven -pl aurora-server "-Dspring-boot.run.profiles=mcp" spring-boot:run
