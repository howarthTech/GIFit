# Local JVM tests

Run with the project's existing Gradle/JUnit 4 tooling (JDK 17+ and Android SDK 36):

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

On macOS/Linux use `./gradlew :app:testDebugUnitTest`.
The first run needs access to download Gradle and any uncached dependencies.
No emulator or device is needed. Existing tests in `app/src/androidTest` are a
separate device suite; the new tests live in `app/src/test`.

`GifEncodingTest` checks GIF header/dimensions, palette padding/truncation,
frame delay/disposal, empty LZW output, and GIF encoding decoded independently
by the JDK's ImageIO reader (including every output pixel).

Sandbox verification (2026-11-19): the Gradle wrapper download was blocked by
restricted network access. Compiling `GifWriter.kt`, `LzwEncoder.kt` and
`GifEncodingTest.kt` with the cached Kotlin JVM compiler and running
`org.junit.runner.JUnitCore com.gifit.app.gif.GifEncodingTest` with JUnit 4.13.2
returned `OK (5 tests)`. The full Gradle task remains pending in that environment.
