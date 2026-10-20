plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
}

// LibGDX's Android backend needs the native .so library inside every APK ABI.
val gdxNatives by configurations.creating

android {
    namespace = "com.horizon.secrets"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.horizon.secrets"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            val storePath = providers.gradleProperty("HORIZON_STORE_FILE").orNull
            if (storePath != null) {
                signingConfig = signingConfigs.create("horizonRelease") {
                    storeFile = file(storePath)
                    storePassword = providers.gradleProperty("HORIZON_STORE_PASSWORD").orNull
                    keyAlias = providers.gradleProperty("HORIZON_KEY_ALIAS").orNull
                    keyPassword = providers.gradleProperty("HORIZON_KEY_PASSWORD").orNull
                }
            }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    sourceSets.getByName("main").jniLibs.srcDir("$buildDir/generated/gdxJniLibs")
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("io.ktor:ktor-client-core:2.3.12")
    implementation("io.ktor:ktor-client-okhttp:2.3.12")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("com.badlogicgames.gdx:gdx:1.13.5")
    implementation("com.badlogicgames.gdx:gdx-backend-android:1.13.5")
    add(gdxNatives.name, "com.badlogicgames.gdx:gdx-platform:1.13.5:natives-armeabi-v7a")
    add(gdxNatives.name, "com.badlogicgames.gdx:gdx-platform:1.13.5:natives-arm64-v8a")
    add(gdxNatives.name, "com.badlogicgames.gdx:gdx-platform:1.13.5:natives-x86")
    add(gdxNatives.name, "com.badlogicgames.gdx:gdx-platform:1.13.5:natives-x86_64")
}

val extractGdxNatives by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/gdxJniLibs")
    inputs.files(gdxNatives)
    outputs.dir(outputDir)

    doLast {
        val targetRoot = outputDir.get().asFile
        targetRoot.deleteRecursively()
        var extracted = 0
        gdxNatives.files.forEach { archive ->
            val abi = archive.name.substringAfterLast("natives-").removeSuffix(".jar")
            project.copy {
                from(zipTree(archive)) { include("libgdx.so") }
                into(targetRoot.resolve(abi))
            }
            if (targetRoot.resolve(abi).resolve("libgdx.so").isFile) extracted += 1
        }
        check(extracted == 4) { "Expected four LibGDX native libraries, extracted $extracted" }
    }
}

tasks.named("preBuild").configure { dependsOn(extractGdxNatives) }
