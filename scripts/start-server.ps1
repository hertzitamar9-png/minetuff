param(
    [int]$MinRamGB = 8,
    [int]$MaxRamGB = 16
)

$ErrorActionPreference = "Stop"
$RepoRoot = Split-Path -Parent $PSScriptRoot
$Runtime = Join-Path $RepoRoot "runtime"
$FoliaJar = Join-Path $Runtime "folia.jar"
$EulaPath = Join-Path $Runtime "eula.txt"

if (-not (Test-Path $FoliaJar)) {
    throw "runtime/folia.jar is missing. Run scripts/setup-server.ps1 first."
}
if (-not (Test-Path $EulaPath)) {
    throw "runtime/eula.txt is missing. Run scripts/setup-server.ps1 first."
}
$Eula = Get-Content $EulaPath -Raw
if ($Eula -notmatch '(?m)^eula=true\s*$') {
    throw "Mojang EULA has not been accepted. Review it and set eula=true in runtime/eula.txt if you accept."
}
if ($MinRamGB -lt 2 -or $MaxRamGB -lt $MinRamGB) {
    throw "RAM settings are invalid. MaxRamGB must be >= MinRamGB and MinRamGB >= 2."
}

$Java = Get-Command java -ErrorAction SilentlyContinue
if (-not $Java) { throw "Java is not on PATH. Minecraft 1.21.11 requires Java 21." }

Push-Location $Runtime
try {
    & $Java.Source "-Xms${MinRamGB}G" "-Xmx${MaxRamGB}G" -XX:+UseG1GC -XX:+ParallelRefProcEnabled `
        -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC `
        -jar $FoliaJar --nogui
} finally {
    Pop-Location
}
