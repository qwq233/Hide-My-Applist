import com.android.build.api.dsl.ApplicationExtension
import com.android.build.gradle.BaseExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.konan.properties.Properties

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.2.1")
    }
}

plugins {
    id("com.android.application") apply false
    id("com.android.library") apply false
    kotlin("android") apply false
}

fun String.execute(currentWorkingDir: File = file("./")): String {
    val byteOut = java.io.ByteArrayOutputStream()
    project.exec {
        workingDir = currentWorkingDir
        commandLine = split("\\s".toRegex())
        standardOutput = byteOut
    }
    return String(byteOut.toByteArray()).trim()
}

val minSdkVer by extra(24)
val targetSdkVer by extra(33)

val appVerName by extra("3.0.0")
val appVerCode by extra(75)
val serviceVer by extra(75)
val minBackupVer by extra(65)

val androidSourceCompatibility = JavaVersion.VERSION_11
val androidTargetCompatibility = JavaVersion.VERSION_11

val gitCommitCount by extra("git rev-list HEAD --count".execute())
val gitCommitHash by extra("git rev-parse --verify --short HEAD".execute())

val localProperties = Properties()
localProperties.load(file("local.properties").inputStream())

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

fun Project.configureBaseExtension() {
    extensions.findByType<BaseExtension>()?.run {
        compileSdkVersion(targetSdkVer)

        defaultConfig {
            minSdk = minSdkVer
            targetSdk = targetSdkVer
            versionCode = appVerCode
            versionName = appVerName
            if (localProperties.getProperty("buildWithGitSuffix").toBoolean())
                versionNameSuffix = ".r${gitCommitCount}.${gitCommitHash}"

            consumerProguardFiles("proguard-rules.pro")
        }

        val signingCfg = signingConfigs.create("config") {
            storeFile = rootProject.file(localProperties.getProperty("fileDir"))
            storePassword = localProperties.getProperty("storePassword")
            keyAlias = localProperties.getProperty("keyAlias")
            keyPassword = localProperties.getProperty("keyPassword")
        }

        buildTypes {
            all {
                signingConfig = signingCfg
            }
            named("release") {
                isMinifyEnabled = true
                proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            }
        }

        compileOptions {
            sourceCompatibility = androidSourceCompatibility
            targetCompatibility = androidTargetCompatibility
        }
    }

    extensions.findByType<ApplicationExtension>()?.run {
        buildTypes {
            named("release") {
                isShrinkResources = true
            }
        }
    }

    extensions.findByType<KotlinCompile>()?.run {
        kotlinOptions {
            jvmTarget = "11"
        }
    }
}

subprojects {
    plugins.withId("com.android.application") {
        configureBaseExtension()
    }
    plugins.withId("com.android.library") {
        configureBaseExtension()
    }
}
