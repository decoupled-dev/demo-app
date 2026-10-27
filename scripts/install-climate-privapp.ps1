# Install com.test.design as a privileged system app so CONTROL_CAR_CLIMATE and the car property permissions
# are granted.
# Requires: rooted userdebug device, adb remount, platform-signed APK (AOSP keys).
#
# Usage (from repo root, after assembleDebug):
#   .\scripts\install-climate-privapp.ps1
#   .\scripts\install-climate-privapp.ps1 -ApkPath path\to\app-debug.apk

param(
    [string]$ApkPath = "",
    [string]$KeyDir = ""
)

$ErrorActionPreference = "Stop"
$RepoRoot = Split-Path -Parent $PSScriptRoot

if (-not $ApkPath) {
    $ApkPath = Join-Path $RepoRoot "app\build\outputs\apk\debug\app-debug.apk"
}
if (-not (Test-Path $ApkPath)) {
    throw "APK not found: $ApkPath - run .\gradlew assembleDebug first"
}

if (-not $KeyDir) {
    $KeyDir = Join-Path $RepoRoot ".tmp-aosp-keys"
}
$Pk8 = Join-Path $KeyDir "platform.pk8"
$Pem = Join-Path $KeyDir "platform.x509.pem"
if (-not (Test-Path $Pk8) -or -not (Test-Path $Pem)) {
    throw "Missing platform keys at $KeyDir. Place platform.pk8 and platform.x509.pem there (AOSP test keys)."
}

$sdk = $env:ANDROID_HOME
if (-not $sdk) { $sdk = $env:ANDROID_SDK_ROOT }
if (-not $sdk) { $sdk = "$env:LOCALAPPDATA\Android\Sdk" }
$apksigner = Get-ChildItem "$sdk\build-tools" -Recurse -Filter apksigner.bat |
    Sort-Object FullName -Descending | Select-Object -First 1
if (-not $apksigner) { throw "apksigner not found under $sdk\build-tools" }

$signed = Join-Path $env:TEMP "com.test.design-platform-signed.apk"
Write-Host "Signing with platform key..."
& $apksigner.FullName sign --key $Pk8 --cert $Pem --out $signed $ApkPath
if ($LASTEXITCODE -ne 0) { throw "apksigner sign failed" }
& $apksigner.FullName verify --verbose $signed | Out-Null

$permXml = Join-Path $RepoRoot "scripts\privapp-permissions-com.test.design.xml"
if (-not (Test-Path $permXml)) { throw "Missing $permXml" }

Write-Host "Preparing device (root + remount)..."
adb root | Out-Null
Start-Sleep -Seconds 2
adb remount | Out-Null

# Remove data install so the priv-app becomes the sole package.
adb uninstall com.test.design 2>$null | Out-Null

Write-Host "Pushing priv-app + permissions allowlist..."
adb shell "mkdir -p /system/priv-app/Design"
adb push $signed /system/priv-app/Design/Design.apk
adb push $permXml /system/etc/permissions/privapp-permissions-com.test.design.xml
adb shell "chmod 644 /system/priv-app/Design/Design.apk /system/etc/permissions/privapp-permissions-com.test.design.xml"

Write-Host "Rebooting (normal Android boot)..."
adb reboot
Write-Host "Done. After boot, confirm with:"
Write-Host "  adb shell dumpsys package com.test.design | findstr CONTROL_CAR_CLIMATE"
