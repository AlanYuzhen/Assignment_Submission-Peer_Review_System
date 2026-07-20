$ErrorActionPreference = "Stop"

$ProjectRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$ClassDir = Join-Path $ProjectRoot "build\classes"

& (Join-Path $PSScriptRoot "build.ps1") -ClassDir $ClassDir
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

Push-Location $ProjectRoot
try {
    & java -cp $ClassDir com.yuzhen.assignment.App
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
} finally {
    Pop-Location
}
