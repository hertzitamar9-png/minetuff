param(
    [string]$MinecraftVersion = "1.21.11"
)

$ErrorActionPreference = "Stop"
$RepoRoot = Split-Path -Parent $PSScriptRoot
$Runtime = Join-Path $RepoRoot "runtime"
$Plugins = Join-Path $Runtime "plugins"
$Headers = @{ "User-Agent" = "MineTuff/0.1 (https://github.com/hertzitamar9-png/minetuff)" }

New-Item -ItemType Directory -Force -Path $Runtime, $Plugins | Out-Null

Write-Host "Building MineTuff plugin..."
Push-Location $RepoRoot
try {
    mvn -B -ntp package
    if ($LASTEXITCODE -ne 0) { throw "Maven build failed." }
} finally {
    Pop-Location
}

$PluginJar = Get-ChildItem (Join-Path $RepoRoot "target\minetuff-mines-*.jar") | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $PluginJar) { throw "Built MineTuff JAR was not found." }
Copy-Item $PluginJar.FullName (Join-Path $Plugins "MineTuff.jar") -Force

Write-Host "Resolving stable Folia $MinecraftVersion build..."
$BuildsUrl = "https://fill.papermc.io/v3/projects/folia/versions/$MinecraftVersion/builds"
$Builds = Invoke-RestMethod -Uri $BuildsUrl -Headers $Headers
$Stable = @($Builds | Where-Object { $_.channel -eq "STABLE" }) | Select-Object -First 1
if (-not $Stable) { throw "No stable Folia build is available for Minecraft $MinecraftVersion." }
$Download = $Stable.downloads.'server:default'
if (-not $Download.url) { throw "Folia build did not expose a server download URL." }

$FoliaJar = Join-Path $Runtime "folia.jar"
Invoke-WebRequest -Uri $Download.url -Headers $Headers -OutFile $FoliaJar

Copy-Item (Join-Path $RepoRoot "server\server.properties") (Join-Path $Runtime "server.properties") -Force
$EulaPath = Join-Path $Runtime "eula.txt"
if (-not (Test-Path $EulaPath)) {
    Set-Content -Path $EulaPath -Value "eula=false" -Encoding ascii
}

Write-Host "Runtime prepared in $Runtime"
Write-Host "Read Mojang's EULA, change runtime/eula.txt to eula=true if you accept it, then run scripts/start-server.ps1."
