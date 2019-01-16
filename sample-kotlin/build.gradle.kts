import org.jetbrains.kotlin.config.KotlinCompilerVersion

plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    compileSdkVersion(28)

    defaultConfig {
        applicationId = "com.polidea.rxandroidble.sample"
        minSdkVersion(18)
        targetSdkVersion(28)
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    sourceSets {
        getByName("main").java.srcDirs("src/main/kotlin")
    }
}

configurations.all {
    resolutionStrategy.cacheChangingModulesFor(0, "seconds")
}

dependencies {
    // RxAndroidBle
    implementation(project(":rxandroidble"))

    // Kotlin stdlib
    implementation(kotlin("stdlib-jdk7", KotlinCompilerVersion.VERSION))

    // AndroidX
    implementation("androidx.appcompat:appcompat:1.0.2")
    implementation("com.google.android.material:material:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.0.0")
    implementation("androidx.annotation:annotation:1.0.1")

    // Butterknife
    implementation("com.jakewharton:butterknife:8.8.1")
    annotationProcessor("com.jakewharton:butterknife-compiler:8.8.1")

    // RxBinding
    implementation("com.jakewharton.rxbinding2:rxbinding:2.2.0")

    // RxJava
    implementation((rootProject.extra.get("libs") as Map<*, *>)["rxjava"] ?: "")
    implementation((rootProject.extra.get("libs") as Map<*, *>)["rxandroid"] ?: "")

    // Replaying share
    implementation("com.jakewharton.rx2:replaying-share:2.1.0")
}
