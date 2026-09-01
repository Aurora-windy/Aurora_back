param(
    [int]$Port = 1208,
    [switch]$Build
)

$ErrorActionPreference = "Stop"

# 启动独立的 MCP Server 副进程，供主进程 MCP Client 做真实远程联调。
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

Write-Host "Starting AURORA MCP peer server on port $Port..."
Write-Host "Bearer token: $env:AURORA_MCP_BEARER_TOKEN"
Write-Host "Maven: $maven"

$jar = Join-Path $PSScriptRoot "aurora-server\target\aurora-server.jar"
$jacksonAnnotations = Join-Path $env:USERPROFILE ".m2\repository\com\fasterxml\jackson\core\jackson-annotations\2.20\jackson-annotations-2.20.jar"
if (-not (Test-Path -LiteralPath $jacksonAnnotations)) {
    throw "MCP 2.0.0 requires jackson-annotations 2.20: $jacksonAnnotations"
}
if ($Build -or -not (Test-Path -LiteralPath $jar)) {
    Write-Host "Building backend modules..."
    & $maven -pl aurora-server -am -DskipTests package
    if ($LASTEXITCODE -ne 0) {
        throw "Backend build failed with exit code $LASTEXITCODE. Run without -Build to use the existing packaged jar."
    }
} else {
    Write-Host "Using existing packaged backend: $jar"
}

# Put the MCP-required Jackson 2.20 annotations ahead of Spring Boot's 2.17.x copy.
& "$env:JAVA_HOME\bin\java.exe" -cp "$jacksonAnnotations;$jar" org.springframework.boot.loader.launch.JarLauncher "--spring.profiles.active=mcp" "--server.port=$Port" "--aurora.ai.mcp.client.startup-sync-enabled=false"
