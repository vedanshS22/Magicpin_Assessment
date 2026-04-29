param(
  [Parameter(ValueFromRemainingArguments = $true)]
  [string[]] $Args
)

$ErrorActionPreference = "Stop"

$GradleVersion = "8.12.1"
$Dist = "gradle-$GradleVersion-bin.zip"
$DistUrl = "https://services.gradle.org/distributions/$Dist"

$BaseDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $BaseDir

$CacheDir = Join-Path $BaseDir ".gradle/bootstrap"
$ZipPath = Join-Path $CacheDir $Dist
$DistDir = Join-Path $CacheDir "gradle-$GradleVersion"
$GradleExe = Join-Path $DistDir "bin/gradle.bat"

# Gradle itself must run on a supported JDK. Your system Java may be newer (e.g., 26) and break Gradle/Groovy.
$JdkMajor = 21
$JdkApi = "https://api.adoptium.net/v3/binary/latest/$JdkMajor/ga/windows/x64/jdk/hotspot/normal/eclipse"
$JdkDir = Join-Path $CacheDir "jdk-$JdkMajor"
$JdkZip = Join-Path $CacheDir "temurin-jdk-$JdkMajor.zip"

function Ensure-Jdk {
  if (Test-Path (Join-Path $JdkDir "bin\java.exe")) {
    return
  }
  New-Item -ItemType Directory -Force -Path $JdkDir | Out-Null
  if (-not (Test-Path $JdkZip)) {
    Write-Host "Downloading Temurin JDK $JdkMajor for Gradle..."
    Invoke-WebRequest -UseBasicParsing -Uri $JdkApi -OutFile $JdkZip
  }
  $tmp = Join-Path $CacheDir "_jdk_extract_$JdkMajor"
  if (Test-Path $tmp) { Remove-Item -Recurse -Force $tmp }
  New-Item -ItemType Directory -Force -Path $tmp | Out-Null
  Write-Host "Extracting JDK..."
  Expand-Archive -Force -Path $JdkZip -DestinationPath $tmp

  $sub = Get-ChildItem -Path $tmp -Directory | Select-Object -First 1
  if (-not $sub) { throw "JDK zip extraction failed" }

  Copy-Item -Recurse -Force -Path (Join-Path $sub.FullName '*') -Destination $JdkDir
  Remove-Item -Recurse -Force $tmp

  if (-not (Test-Path (Join-Path $JdkDir "bin\java.exe"))) {
    throw "JDK bootstrap failed. Expected java.exe under $JdkDir\bin"
  }
}

function Ensure-Gradle {
  New-Item -ItemType Directory -Force -Path $CacheDir | Out-Null
  if (-not (Test-Path $GradleExe)) {
    if (-not (Test-Path $ZipPath)) {
      Write-Host "Downloading Gradle $GradleVersion..."
      Invoke-WebRequest -UseBasicParsing -Uri $DistUrl -OutFile $ZipPath
    }
    Write-Host "Extracting Gradle..."
    Expand-Archive -Force -Path $ZipPath -DestinationPath $CacheDir
  }
  if (-not (Test-Path $GradleExe)) {
    throw "Gradle bootstrap failed. Expected $GradleExe"
  }
}

Ensure-Jdk
Ensure-Gradle

$env:JAVA_HOME = $JdkDir
$env:Path = (Join-Path $JdkDir 'bin') + ';' + $env:Path

& $GradleExe @Args
exit $LASTEXITCODE