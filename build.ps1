$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$cp = (Get-Content -LiteralPath (Join-Path $root 'cp.txt') -Raw).Trim()

# The bundled tools\jre is not usable on every machine (it can fail with
# "could not open lib\jvm.cfg"), so pick the first runtime that actually runs.
function Test-JavaHome([string]$jdkDir) {
    if ([string]::IsNullOrWhiteSpace($jdkDir)) { return $false }
    $exe = Join-Path $jdkDir 'bin\java.exe'
    if (-not (Test-Path -LiteralPath $exe)) { return $false }
    # java -version writes to stderr; with ErrorActionPreference=Stop that is
    # turned into a terminating error, so relax it while probing.
    $previous = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        & $exe -version 2>&1 | Out-Null
        return ($LASTEXITCODE -eq 0)
    } catch {
        return $false
    } finally {
        $ErrorActionPreference = $previous
    }
}

$candidates = @(
    (Join-Path $root '..\tools\jre'),
    (Join-Path $env:APPDATA '.minecraft\runtime\java-runtime-delta'),
    $env:JAVA_HOME
)
$javaHome = $null
foreach ($candidate in $candidates) {
    if (Test-JavaHome $candidate) {
        $javaHome = (Resolve-Path -LiteralPath $candidate).Path
        break
    }
}
if (-not $javaHome) {
    throw 'No working Java runtime found. Tried tools\jre, %APPDATA%\.minecraft\runtime\java-runtime-delta and JAVA_HOME.'
}
$java = Join-Path $javaHome 'bin\java.exe'
Write-Host "Using Java: $javaHome"

$classes = Join-Path $root 'build\classes'
if (Test-Path -LiteralPath $classes) { Remove-Item -LiteralPath $classes -Recurse -Force }
$null = New-Item -ItemType Directory -Force -Path $classes
$src = Get-ChildItem -LiteralPath (Join-Path $root 'src\main\java') -Recurse -Filter '*.java' | Select-Object -ExpandProperty FullName

$javac = Join-Path $javaHome 'bin\javac.exe'
if (Test-Path -LiteralPath $javac) {
    & $javac -proc:none -encoding UTF-8 -cp $cp -d $classes $src
} else {
    # JRE images ship jdk.compiler without the javac launcher.
    & $java -m jdk.compiler/com.sun.tools.javac.Main -proc:none -encoding UTF-8 -cp $cp -d $classes $src
}
if ($LASTEXITCODE -ne 0) { throw "javac failed: $LASTEXITCODE" }

# Version lives in neoforge.mods.toml only, so bumping it is a one-line change.
$toml = Get-Content -LiteralPath (Join-Path $root 'src\main\resources\META-INF\neoforge.mods.toml') -Raw
# Anchored to the line start so it cannot match loaderVersion="...".
if ($toml -notmatch '(?m)^version\s*=\s*"([^"]+)"') { throw 'Could not read the mod version from neoforge.mods.toml' }
$version = $Matches[1]
$out = Join-Path $root "dist\mcreconstruction-$version.jar"
if (Test-Path -LiteralPath $out) { Remove-Item -LiteralPath $out -Force }
$jarExe = Join-Path $javaHome 'bin\jar.exe'
if (Test-Path -LiteralPath $jarExe) {
    & $jarExe --create --file $out -C $classes . -C (Join-Path $root 'src\main\resources') .
} else {
    & $java -m jdk.jartool/sun.tools.jar.Main --create --file $out -C $classes . -C (Join-Path $root 'src\main\resources') .
}
if ($LASTEXITCODE -ne 0) { throw "jar failed: $LASTEXITCODE" }
Write-Host "Built $out"
