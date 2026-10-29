import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.android.application) apply false
}

// The Android target is only configured when an Android SDK is installed, so the
// desktop (Linux) target still builds on a machine without one.
val androidSdkDir: String? = run {
    val props = rootProject.file("local.properties")
    val fromProps = props.takeIf { it.exists() }?.readLines()
        ?.firstOrNull { it.startsWith("sdk.dir=") }?.substringAfter("=")
    val fromEnv = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
    val default = "${System.getProperty("user.home")}/Android/Sdk"
    listOfNotNull(fromProps, fromEnv, default).firstOrNull { file(it).isDirectory }
}
val withAndroid = androidSdkDir != null

if (withAndroid) {
    // AGP only looks at local.properties and ANDROID_HOME, not the default SDK folder
    val props = rootProject.file("local.properties")
    if (!props.exists()) props.writeText("sdk.dir=$androidSdkDir\n")
    apply(plugin = "com.android.application")
}

kotlin {
    if (withAndroid) {
        androidTarget {
            compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
        }
    }
    jvm("desktop") {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }

    sourceSets {
        all {
            languageSettings.optIn("kotlin.time.ExperimentalTime")
            languageSettings.optIn("androidx.compose.material3.ExperimentalMaterial3Api")
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.cio)
            implementation(libs.ktor.network)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        if (withAndroid) {
            androidMain.dependencies {
                implementation(libs.androidx.activity.compose)
                implementation(libs.androidx.core.ktx)
                implementation(libs.kotlinx.coroutines.android)
            }
        }
        val desktopMain by getting
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
        }
    }
}

if (withAndroid) {
    extensions.configure<com.android.build.api.dsl.ApplicationExtension> {
        namespace = "app.actioncam"
        compileSdk = 36
        defaultConfig {
            applicationId = "app.actioncam"
            minSdk = 26
            targetSdk = 36
            versionCode = 1
            versionName = "0.1.0"
        }
        buildTypes {
            getByName("release") {
                isMinifyEnabled = false
                // personal app: sign release builds with the debug key so they install directly
                signingConfig = signingConfigs.getByName("debug")
            }
        }
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
        packaging {
            resources.excludes += "/META-INF/{AL2.0,LGPL2.1,INDEX.LIST,io.netty.versions.properties}"
        }
    }
}

compose.desktop {
    application {
        mainClass = "app.actioncam.MainKt"
    }
}

// ./gradlew :composeApp:screenshots renders every screen with sample data to build/screenshots
tasks.register<JavaExec>("screenshots") {
    val main = kotlin.jvm("desktop").compilations.getByName("main")
    dependsOn(main.compileAllTaskName)
    classpath = files(main.output.allOutputs, main.runtimeDependencyFiles)
    mainClass.set("app.actioncam.ScreenshotsKt")
    args(layout.buildDirectory.dir("screenshots").get().asFile.absolutePath)
}
