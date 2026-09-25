plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)

}

android {
    namespace = "com.synaptia.smartmoda.core.crypto"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    sourceSets["main"].kotlin.srcDir("src/main/kotlin")
    sourceSets["test"].kotlin.srcDir("src/test/kotlin")
}

kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

dependencies {
    implementation(project(":android:core:common"))
    implementation(libs.coroutines.core)
    implementation(libs.androidx.security.crypto)

    testImplementation(libs.junit5.jupiter)
    testImplementation(libs.kotest.assertions)
    testRuntimeOnly(libs.junit5.launcher)
}

tasks.withType<Test> { useJUnitPlatform() }
