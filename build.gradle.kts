buildscript {
    repositories {
        google()
        mavenCentral()
        // StringFog artifacts are published via JitPack; keep this in buildscript
        // too because the legacy buildscript classpath is resolved separately
        // from dependencyResolutionManagement in settings.gradle.kts.
        maven("https://jitpack.io")
    }
    dependencies {
        classpath("com.github.megatronking.stringfog:gradle-plugin:5.2.0")
        classpath("com.github.megatronking.stringfog:xor:5.0.0")
        classpath("com.android.tools.build:gradle:8.2.2")
    }
}
