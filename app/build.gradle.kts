import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val releaseBuildRequested = gradle.startParameter.taskNames.any { taskName ->
    taskName.contains("Release", ignoreCase = true) ||
        taskName.endsWith(":assemble", ignoreCase = true) ||
        taskName == "assemble" ||
        taskName.endsWith(":build", ignoreCase = true) ||
        taskName == "build"
}
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use(::load)
    } else if (releaseBuildRequested) {
        throw GradleException(
            "Release signing requires a local keystore.properties file. " +
                "Copy keystore.properties.example and point it at the local release keystore."
        )
    }
}

android {
    namespace = "com.clearcart.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.clearcart.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 7
        versionName = "0.1.6"
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                val requiredKeys = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
                val missingKeys = requiredKeys.filter { keystoreProperties.getProperty(it).isNullOrBlank() }
                if (missingKeys.isNotEmpty()) {
                    throw GradleException(
                        "keystore.properties is missing required release signing fields: " +
                            missingKeys.joinToString()
                    )
                }

                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.06.01"))
    implementation("androidx.activity:activity-compose:1.12.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.datastore:datastore-preferences:1.2.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.navigation:navigation-compose:2.9.0")
    implementation("androidx.room:room-ktx:2.7.2")
    implementation("androidx.room:room-runtime:2.7.2")
    ksp("androidx.room:room-compiler:2.7.2")
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("androidx.camera:camera-camera2:1.5.0")
    implementation("androidx.camera:camera-lifecycle:1.5.0")
    implementation("androidx.camera:camera-view:1.5.0")
    implementation("io.coil-kt.coil3:coil-compose:3.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    testImplementation("junit:junit:4.13.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
