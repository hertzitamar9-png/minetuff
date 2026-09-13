$ErrorActionPreference = "Stop"
$Files = @(
    (Join-Path $PSScriptRoot "setup-server.ps1"),
    (Join-Path $PSScriptRoot "start-server.ps1")
)

$Failed = $false
foreach ($File in $Files) {
    $Tokens = $null
    $Errors = $null
    [void][System.Management.Automation.Language.Parser]::ParseFile($File, [ref]$Tokens, [ref]$Errors)
    if ($Errors.Count -gt 0) {
        $Failed = $true
        Write-Error "PowerShell parse errors in $File"
        foreach ($ParseError in $Errors) {
            Write-Error $ParseError.Message
        }
    } else {
        Write-Host "OK: $File"
    }
}

if ($Failed) { exit 1 }
