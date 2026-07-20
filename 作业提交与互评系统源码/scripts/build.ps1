param(
    [string]$SourceRoot,
    [string]$ClassDir
)

$ErrorActionPreference = "Stop"

$ProjectRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
if ([string]::IsNullOrWhiteSpace($SourceRoot)) {
    $SourceRoot = Join-Path $ProjectRoot "src\main\java"
}
if ([string]::IsNullOrWhiteSpace($ClassDir)) {
    $ClassDir = Join-Path $ProjectRoot "build\classes"
}

if (-not (Get-Command javac -ErrorAction SilentlyContinue)) {
    throw "javac was not found. Please install a JDK and add it to PATH."
}

New-Item -ItemType Directory -Force -Path $ClassDir | Out-Null

$sources = @(Get-ChildItem -Path $SourceRoot -Recurse -Filter "*.java" | ForEach-Object { $_.FullName })
if ($sources.Count -eq 0) {
    throw "No Java source files found under $SourceRoot."
}

& javac -encoding UTF-8 -d $ClassDir $sources
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

Write-Host "Build succeeded: $ClassDir"
