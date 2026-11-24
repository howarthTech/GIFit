$ErrorActionPreference = 'Stop'
Push-Location (Split-Path $PSScriptRoot -Parent)
try {
    $cacheRoot = Join-Path $env:USERPROFILE '.gradle/caches/modules-2/files-2.1'
    $jars = Get-ChildItem $cacheRoot -Recurse -Filter *.jar | Where-Object {
        $_.FullName -match '\\(org.jetbrains.kotlin|org.jetbrains.kotlinx|org.jetbrains|org.jetbrains.intellij.deps|junit|org.hamcrest)\\'
    }
    $compilerCp = $jars.FullName -join ';'
    New-Item -ItemType Directory -Force build/test-classes | Out-Null
    java -cp $compilerCp org.jetbrains.kotlin.cli.jvm.K2JVMCompiler -no-stdlib -no-reflect -jvm-target 17 -classpath $compilerCp -d build/test-classes app/src/main/java/com/gifit/app/gif/GifWriter.kt app/src/main/java/com/gifit/app/gif/LzwEncoder.kt app/src/test/java/com/gifit/app/gif/GifEncodingTest.kt
    if ($LASTEXITCODE -ne 0) { throw 'Kotlin test compilation failed.' }
    java -cp "build/test-classes;$compilerCp" org.junit.runner.JUnitCore com.gifit.app.gif.GifEncodingTest
    if ($LASTEXITCODE -ne 0) { throw 'JUnit tests failed.' }
} finally {
    Pop-Location
}
