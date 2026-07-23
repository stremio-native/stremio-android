import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("com.google.firebase.firebase-perf")
}

fun stringPropertyOrEnv(name: String): String? =
    (findProperty(name) as? String)?.takeIf { it.isNotBlank() }
        ?: System.getenv(name)?.takeIf { it.isNotBlank() }

val supportedAbis = listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")

val releaseSigningValues = listOf(
    stringPropertyOrEnv("ANDROID_KEYSTORE_FILE"),
    stringPropertyOrEnv("ANDROID_KEYSTORE_PASSWORD"),
    stringPropertyOrEnv("ANDROID_KEY_ALIAS"),
    stringPropertyOrEnv("ANDROID_KEY_PASSWORD"),
)
val hasPartialReleaseSigning = releaseSigningValues.any { it != null } && releaseSigningValues.any { it == null }
if (hasPartialReleaseSigning) {
    throw GradleException(
        "Release signing requires ANDROID_KEYSTORE_FILE, ANDROID_KEYSTORE_PASSWORD, ANDROID_KEY_ALIAS, and ANDROID_KEY_PASSWORD.",
    )
}

val localPropertiesFile = rootProject.file("local.properties")
val localProperties = Properties()
if (localPropertiesFile.exists()) {
    val stream = localPropertiesFile.inputStream()
    localProperties.load(stream)
    stream.close()
}
val posthogApiKey = (localProperties.getProperty("posthog.apiKey") ?: System.getenv("POSTHOG_API_KEY") ?: "").trim()
val posthogHost = (localProperties.getProperty("posthog.host") ?: System.getenv("POSTHOG_HOST") ?: "https://us.i.posthog.com").trim()

android {
    namespace = "com.stremio.mobile"
    compileSdk = 37
    ndkVersion = "29.0.13846066"

    packaging {
        resources {
            excludes.add("google/protobuf/**")
            excludes.add("META-INF/gradle/incremental.annotation.processors")
        }
    }

    defaultConfig {
        applicationId = "com.stremio.mobile"
        minSdk = 24
        targetSdk = 37
        versionCode = stringPropertyOrEnv("VERSION_CODE")?.toIntOrNull() ?: 1
        versionName = stringPropertyOrEnv("VERSION_NAME") ?: "0.1.0"
        ndk {
            abiFilters.addAll(supportedAbis)
        }
        buildConfigField("String", "POSTHOG_API_KEY", "\"$posthogApiKey\"")
        buildConfigField("String", "POSTHOG_HOST", "\"$posthogHost\"")
    }

    splits {
        abi {
            isEnable = true
            reset()
            include(*supportedAbis.toTypedArray())
            isUniversalApk = true
        }
    }

    signingConfigs {
        if (!hasPartialReleaseSigning && releaseSigningValues.all { it != null }) {
            create("release") {
                storeFile = file(releaseSigningValues[0]!!)
                storePassword = releaseSigningValues[1]
                keyAlias = releaseSigningValues[2]
                keyPassword = releaseSigningValues[3]
            }
        }
    }

    buildTypes {
        debug {
            matchingFallbacks.add("release")
            signingConfigs.findByName("release")?.let {
                signingConfig = it
            }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }


}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.05.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.core:core-splashscreen:1.2.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("io.coil-kt.coil3:coil-compose:3.4.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.4.0")

    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0-rc01")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0-rc01")
    implementation("androidx.navigation:navigation-compose:2.9.8")
    implementation("androidx.fragment:fragment:1.5.4")

    implementation("com.facebook.android:facebook-login:18.2.3")

    implementation(files("libs/rustls-platform-verifier-0.1.1.aar"))

    implementation("androidx.media3:media3-exoplayer:1.10.1")
    implementation("androidx.media3:media3-ui:1.10.1")
    implementation("androidx.media3:media3-session:1.10.1")
    implementation(project(":mpv-android-lib"))

    implementation("io.github.kyant0:backdrop:2.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.4.0")
    implementation("pro.streem.pbandk:pbandk-runtime:0.16.0")
    implementation("com.github.Stremio:stremio-core-kotlin:1.15.0")
    implementation("com.jakewharton.timber:timber:5.0.1")
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.14")
    implementation(platform("com.google.firebase:firebase-bom:34.15.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-perf")
    implementation("com.posthog:posthog-android:3.56.2")
    testImplementation("junit:junit:4.13.2")
}
data class StreamServerTarget(
    val taskSuffix: String,
    val abi: String,
    val rustTarget: String,
    val vcpkgTriplet: String,
    val vcpkgInstallRootName: String,
    val vcpkgInstalledEnvSuffix: String,
)

val streamServerTargets = listOf(
    StreamServerTarget("Armv7", "armeabi-v7a", "armv7-linux-androideabi", "arm-android", "arm", "ARMV7"),
    StreamServerTarget("Arm64", "arm64-v8a", "aarch64-linux-android", "arm64-android", "arm64", "ARM64"),
    StreamServerTarget("X86", "x86", "i686-linux-android", "x86-android", "x86", "X86"),
    StreamServerTarget("X86_64", "x86_64", "x86_64-linux-android", "x64-android", "x64", "X86_64"),
)

val externalStreamServerRoot = projectDir.resolve("../../stream-server").normalize()
val submoduleStreamServerRoot = rootProject.file("stream-server")
val streamServerRoot = when {
    externalStreamServerRoot.resolve("server/Cargo.toml").isFile -> externalStreamServerRoot
    submoduleStreamServerRoot.resolve("server/Cargo.toml").isFile -> submoduleStreamServerRoot
    else -> externalStreamServerRoot
}
val vcpkgRoot = stringPropertyOrEnv("VCPKG_ROOT") ?: "C:\\vcpkg"

streamServerTargets.forEach { target ->
    tasks.register<Exec>("buildStreamServer${target.taskSuffix}") {
        workingDir = streamServerRoot.resolve("server")
        val cargoArgs = listOf(
            "cargo",
            "ndk",
            "--target",
            target.rustTarget,
            "--platform",
            "24",
            "build",
            "--release",
            "--features",
            "libtorrent",
            "--no-default-features",
        )
        if (org.apache.tools.ant.taskdefs.condition.Os.isFamily(org.apache.tools.ant.taskdefs.condition.Os.FAMILY_WINDOWS)) {
            commandLine("cmd", "/c", cargoArgs.joinToString(" "))
        } else {
            commandLine(cargoArgs)
        }

        val targetVcpkgInstalledDir = stringPropertyOrEnv("VCPKG_INSTALLED_DIR_${target.vcpkgInstalledEnvSuffix}")
            ?: stringPropertyOrEnv("VCPKG_INSTALLED_DIR")
            ?: file("$vcpkgRoot/installed-${target.vcpkgInstallRootName}").absolutePath
        val tripletRoot = file(targetVcpkgInstalledDir).resolve(target.vcpkgTriplet)
        environment("VCPKG_ROOT", vcpkgRoot)
        environment("VCPKG_INSTALLED_DIR", targetVcpkgInstalledDir)
        environment("VCPKGRS_TRIPLET", target.vcpkgTriplet)
        environment("OPENSSL_DIR", tripletRoot.absolutePath)
        environment("PKG_CONFIG_ALLOW_CROSS", "1")
        environment("PKG_CONFIG_PATH", tripletRoot.resolve("lib/pkgconfig").absolutePath)
        environment("PKG_CONFIG_SYSROOT_DIR", tripletRoot.absolutePath)
    }
}

tasks.register<Copy>("copyStreamServerJniLibs") {
    dependsOn(streamServerTargets.map { "buildStreamServer${it.taskSuffix}" })
    streamServerTargets.forEach { target ->
        from(streamServerRoot.resolve("target/${target.rustTarget}/release/libstream_server.so")) {
            into(target.abi)
        }
    }
    into("src/main/jniLibs")
}

// Disabled automatic compilation to speed up builds.
// Run compileNativeLibs task to compile native libraries.
// tasks.named("preBuild") {
//     dependsOn("copyStreamServerJniLibs")
// }
