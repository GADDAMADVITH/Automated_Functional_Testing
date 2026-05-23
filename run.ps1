$ErrorActionPreference = "Stop"

$automationDir = $PSScriptRoot
$srcDir = Join-Path $automationDir "src"

if (-not (Test-Path $srcDir)) {
  throw "Cannot find src folder: $srcDir"
}

# Ensure chromedriver.exe is discoverable (fallback for environments where Selenium Manager is blocked)
$chromeDriverDir = Join-Path $automationDir "chromedriver-win64"
if (Test-Path (Join-Path $chromeDriverDir "chromedriver.exe")) {
  $env:PATH = ((Resolve-Path $chromeDriverDir).Path + ";" + $env:PATH)
}

Push-Location $srcDir
try {
  Write-Host "Compiling LaunchTest.java..."
  javac -cp ".;..\lib\*" .\LaunchTest.java

  Write-Host "Running LaunchTest..."
  java -cp ".;..\lib\*" LaunchTest
} finally {
  Pop-Location
}

