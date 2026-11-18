# Gera o APK de teste em dist\. Uso no terminal do VS Code: .\gerar-apk.ps1
$ErrorActionPreference = 'Stop'

$env:JAVA_HOME = (Get-ChildItem 'C:\Program Files\Microsoft' -Directory -Filter 'jdk-21*' | Select-Object -First 1).FullName
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"

& "$PSScriptRoot\gradlew.bat" -p $PSScriptRoot assembleDebug
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

New-Item -ItemType Directory -Force "$PSScriptRoot\dist" | Out-Null
Copy-Item "$env:LOCALAPPDATA\ClinicaEstetica\build\app\outputs\apk\debug\app-debug.apk" `
    "$PSScriptRoot\dist\ClinicaEstetica-debug.apk" -Force
Write-Host "APK pronto em dist\ClinicaEstetica-debug.apk"
