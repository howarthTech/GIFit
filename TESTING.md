# Local JVM tests

Run the existing Gradle/JUnit 4 setup from the repository root:

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

Requires JDK 17 or newer, Android SDK 36, and the Gradle/dependency downloads
(or a populated Gradle cache). No emulator is needed. Reports are written to
`C:/GITit_build/app/reports/tests/testDebugUnitTest/index.html`, per the existing
build configuration.

`GifEncodingTest` covers little-endian dimensions, palette padding, frame delay
and disposal, empty LZW output, and a varied 4096-pixel GIF decoded independently
by JDK ImageIO. Production code is unchanged; JUnit 4.13.2 was already configured.

For restricted offline environments with Kotlin compiler and JUnit artifacts
already in the local Gradle cache, run the same tests directly:

```powershell
.\scripts\test-gif-encoding.ps1
```

This fallback compiles only the two platform-independent production classes and
their tests. It does not verify the full Android build.
